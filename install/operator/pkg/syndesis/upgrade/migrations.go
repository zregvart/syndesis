/*
 * Copyright (C) 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package upgrade

import (
	"fmt"
	"strconv"
	"time"

	"github.com/syndesisio/syndesis/install/operator/pkg"

	k8serrors "k8s.io/apimachinery/pkg/api/errors"

	sbackup "github.com/syndesisio/syndesis/install/operator/pkg/syndesis/backup"

	"github.com/syndesisio/syndesis/install/operator/pkg/syndesis/operation"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/syndesisio/syndesis/install/operator/pkg/apis/syndesis/v1beta2"
	"github.com/syndesisio/syndesis/install/operator/pkg/generator"
	"github.com/syndesisio/syndesis/install/operator/pkg/syndesis/configuration"
	"github.com/syndesisio/syndesis/install/operator/pkg/util"
	batchv1 "k8s.io/api/batch/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/wait"
)

type migration struct {
	step
	jobName  string
	syndesis *v1beta2.Syndesis
	backup   sbackup.Runner
	timeout  time.Duration
	interval time.Duration
}

func newMigration(base step, s *v1beta2.Syndesis, b sbackup.Runner) (m *migration) {
	m = &migration{
		jobName:  "upgrade-db-migration",
		step:     base,
		syndesis: s,
		backup:   b,
		timeout:  time.Second * 240,
		interval: time.Second * 10,
	}
	m.name = "Database migration"
	return
}

/*
 * The upgrade script and mechanism could also be used internally by the syndesis-server
 * application to perform an upgrade during startup. However, this is recommended only
 * for a development setup as there is no easy way to rollback if things go wrong.
 */
func (m *migration) run() (err error) {
	m.executed = true

	err = m.dbMigration()
	if err != nil {
		m.log.Error(err, "error while running migrations", "step", m.name)
	}

	return
}

/*
 * Rollback will ensure that the resources created by the
 * run method are deleted
 *
 * Delete the job and pods generated by the migration
 *
 * Rollback the database with the dump taken before starting the upgrade
 */
func (m *migration) rollback() (err error) {
	m.executed = false

	// delete resources previously generated, dont exit on error
	{
		api, err := m.api()
		if err != nil {
			return err
		}

		m.log.Info("cleaning up migration job")
		if err = api.BatchV1().
			Jobs(m.namespace).
			Delete(m.context, "upgrade-db-migration", metav1.DeleteOptions{}); err != nil {
			if !k8serrors.IsNotFound(err) {
				m.log.Error(err, "there was an error deleting the job generated by the migration step, some manual steps might be required")
			}
		}

		m.log.Info("cleaning up migration pods")
		if err = api.CoreV1().
			Pods(m.namespace).
			DeleteCollection(m.context, metav1.DeleteOptions{}, metav1.ListOptions{LabelSelector: "job-name=upgrade-db-migration"}); err != nil {
			m.log.Error(err, "there was an error deleting the pods generated by the migration step, some manual steps might be required")
		}
	}

	if err = m.backup.Validate(); err != nil {
		if m.backup, err = m.backup.BuildBackupDir(pkg.DefaultOperatorTag); err != nil {
			return
		}
	}

	err = m.backup.RestoreDb()
	return
}

func (m *migration) dbMigration() (err error) {
	// Load configuration to to use as context for generator pkg
	config, err := configuration.GetProperties(m.context, configuration.TemplateConfig, m.clientTools, m.syndesis)
	if err != nil {
		return err
	}

	// Get migration resources, this should be the db migration job
	resources, err := generator.Render("./upgrade", config)
	if err != nil {
		return err
	}

	client, err := m.clientTools.RuntimeClient()
	if err != nil {
		return err
	}

	// install the resources
	for _, res := range resources {
		operation.SetNamespaceAndOwnerReference(res, m.syndesis)
		_, _, err := util.CreateOrUpdate(m.context, client, &res)
		if err != nil {
			return err
		}
	}

	// Wait for migration Job to correctly finish
	err = wait.Poll(m.interval, m.timeout, func() (done bool, err error) {
		j := &batchv1.Job{}
		if err = client.Get(m.context, types.NamespacedName{Namespace: m.namespace, Name: m.jobName}, j); err != nil {
			return false, err
		}

		rt := strconv.FormatInt(time.Now().Unix()-j.Status.StartTime.Unix(), 10) + "s"

		if j.Status.Failed != 0 {
			return false, fmt.Errorf("job failed, %d", j.Status.Failed)
		}

		if j.Status.Succeeded != 0 {
			m.log.Info("database migration job successfully finished", "active jobs", j.Status.Active, "succeeded jobs", j.Status.Succeeded, "running time", rt)
			return true, nil
		}

		m.log.Info("waiting for database migration to finish", "active jobs", j.Status.Active, "running time", rt)
		return false, nil
	})

	return
}

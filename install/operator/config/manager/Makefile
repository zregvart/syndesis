ASSETS := ../../pkg/generator/assets
TMPL := tmpl
YAML := yaml
NAMESPACE_VAR := {NAMESPACE}
USER_VAR := {KUBE_USER}
TAG_VAR := {TAG}
IMAGE_VAR := {IMAGE}
DB_IMAGE_VAR := {DB_IMAGE}

ROLENAME := syndesis-operator
MANAGER := ./manager.gen
MANAGER_INSTALL := ./manager-install.gen

# User customisable variables
DB_IMAGE ?= centos/postgresql-10-centos7
IMAGE ?= quay.io/syndesis/syndesis-operator
NAMESPACE ?= syndesis
TAG ?= 1.12

.PHONY: sync init

# start-sync
#
# Copy the go template from the src directory
# Convert the go template to a formatted yaml file:
# - Delete any line beginning with '{{'
# - Delete any empty lines
# - Convert go-style array '- xxx' to yaml format: '---\n' & remove double-spaces at start of lines
# - Replace Role, Namespace, Tag, Image placeholders
# - Delete DevSupport conditional, ImageStreamTag line, else and end codewords
#
sync:
	cp $(ASSETS)/install/operator_install.yml.tmpl $(MANAGER_INSTALL).$(TMPL)
	sed -i 's/{{ .Role }}/$(ROLENAME)/' $(MANAGER_INSTALL).$(TMPL)
	sed -i 's/{{.Namespace}}/$(NAMESPACE_VAR)/' $(MANAGER_INSTALL).$(TMPL)
	sed -i 's/{{.Tag}}/$(TAG_VAR)/' $(MANAGER_INSTALL).$(TMPL)
	sed -i 's/{{.Image}}/$(IMAGE_VAR)/' $(MANAGER_INSTALL).$(TMPL)
	sed -i '/{{if .DevSupport}}/d' $(MANAGER_INSTALL).$(TMPL)
	sed -i '/kind: ImageStreamTag/d' $(MANAGER_INSTALL).$(TMPL)
	sed -i '/{{- \|{{else}}\|{{end}}\|^{{\|^$$/d' $(MANAGER_INSTALL).$(TMPL)
	sed -i 's/^- /\n---\n/' $(MANAGER_INSTALL).$(TMPL)
	sed -i 's/^  //' $(MANAGER_INSTALL).$(TMPL)
	cp $(ASSETS)/install/operator_deployment.yml.tmpl $(MANAGER).$(TMPL)
	sed -i '/{{- if/,/{{- end}}\|{{end}}/d' $(MANAGER).$(TMPL)
	sed -i 's/{{.Tag}}/$(TAG_VAR)/' $(MANAGER).$(TMPL)
	sed -i 's/{{.Image}}/$(IMAGE_VAR)/' $(MANAGER).$(TMPL)
	sed -i 's/{{.DatabaseImage}}/$(DB_IMAGE_VAR)/' $(MANAGER).$(TMPL)
	sed -i '/{{- \|{{else}}\|{{end}}\|^$$/d' $(MANAGER).$(TMPL)
# end-sync

init: sync
	for resource in $(MANAGER_INSTALL) $(MANAGER); do \
		cp $${resource}.$(TMPL) $${resource}.$(YAML); \
		sed -i 's/$(NAMESPACE_VAR)/$(NAMESPACE)/' $${resource}.$(YAML); \
		sed -i 's/$(TAG_VAR)/$(TAG)/' $${resource}.$(YAML); \
		sed -i 's~$(IMAGE_VAR)~$(IMAGE)~' $${resource}.$(YAML); \
		sed -i 's~$(DB_IMAGE_VAR)~$(DB_IMAGE)~' $${resource}.$(YAML); \
	done

/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.connector.sql.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import io.syndesis.common.model.action.ConnectorAction;
import io.syndesis.common.model.action.ConnectorDescriptor;
import io.syndesis.common.model.connection.Connector;
import io.syndesis.common.model.integration.Step;
import io.syndesis.common.model.integration.StepKind;
import io.syndesis.connector.sql.common.DbEnum;
import io.syndesis.connector.sql.common.JSONBeanUtil;
import io.syndesis.connector.sql.common.SqlTest;
import io.syndesis.connector.sql.common.SqlTest.ConnectionInfo;
import io.syndesis.connector.support.test.ConnectorTestSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SqlTest.class)
public abstract class SqlConnectorTestSupport extends ConnectorTestSupport {

    protected final ConnectionInfo info;

    public SqlConnectorTestSupport(final ConnectionInfo info) {
        this.info = info;
    }

    protected Step newSqlEndpointStep(final ConnectionInfo info, final String actionId, final Consumer<Step.Builder> stepConsumer,
        final Consumer<ConnectorDescriptor.Builder> descriptorConsumer) {
        final Connector connector = getResourceManager().mandatoryLoadConnector("sql");
        final ConnectorAction action = getResourceManager().mandatoryLookupAction(connector, actionId);
        final ConnectorDescriptor.Builder descriptorBuilder = new ConnectorDescriptor.Builder().createFrom(action.getDescriptor());

        descriptorConsumer.accept(descriptorBuilder);

        final Step.Builder builder = new Step.Builder()
            .stepKind(StepKind.endpoint)
            .action(new ConnectorAction.Builder().createFrom(action).descriptor(descriptorBuilder.build()).build())
            .connection(new io.syndesis.common.model.connection.Connection.Builder()
                .connector(connector)
                .putConfiguredProperty("user", info.username)
                .putConfiguredProperty("password", info.password)
                .putConfiguredProperty("url", info.url)
                .build());

        stepConsumer.accept(builder);

        return builder.build();
    }

    protected Step newSqlEndpointStep(final String actionId, final Consumer<Step.Builder> consumer) {
        final Connector connector = getResourceManager().mandatoryLoadConnector("sql");
        final ConnectorAction action = getResourceManager().mandatoryLookupAction(connector, actionId);

        final Step.Builder builder = new Step.Builder()
            .stepKind(StepKind.endpoint)
            .action(action)
            .connection(new io.syndesis.common.model.connection.Connection.Builder()
                .connector(connector)
                .putConfiguredProperty("user", info.username)
                .putConfiguredProperty("password", info.password)
                .putConfiguredProperty("url", info.url)
                .build());

        consumer.accept(builder);

        return builder.build();
    }

    protected static void validateJson(final List<String> jsonBeans, final String propertyName, final String... expectedValues) {
        Assertions.assertEquals(expectedValues.length, jsonBeans.size());

        for (int i = 0; i < expectedValues.length; i++) {
            final Properties properties = JSONBeanUtil.parsePropertiesFromJSONBean(jsonBeans.get(i));
            Assertions.assertEquals(expectedValues[i], properties.get(propertyName).toString());
        }
    }

    protected static void validateProperty(final List<Properties> propertyList, final String propertyName, final String... expectedValues) {
        Assertions.assertEquals(expectedValues.length, propertyList.size());

        for (int i = 0; i < expectedValues.length; i++) {
            Assertions.assertEquals(expectedValues[i], propertyList.get(i).get(propertyName).toString());
        }
    }

    public static DbEnum determineDatabaseTypeFrom(final Connection con) throws SQLException {
        final DatabaseMetaData metaData = con.getMetaData();
        final String databaseProductName = metaData.getDatabaseProductName();
        return DbEnum.fromName(databaseProductName);
    }

    protected static <T> Consumer<T> nop(final Class<T> ofType) {
        return s -> {
            // do nothing
        };
    }
}

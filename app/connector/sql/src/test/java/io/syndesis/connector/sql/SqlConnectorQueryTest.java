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
package io.syndesis.connector.sql;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import io.syndesis.common.model.integration.Step;
import io.syndesis.connector.sql.common.JSONBeanUtil;
import io.syndesis.connector.sql.common.SqlStatementMetaData;
import io.syndesis.connector.sql.common.SqlStatementParser;
import io.syndesis.connector.sql.util.SqlConnectorTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

public class SqlConnectorQueryTest extends SqlConnectorTestSupport {

    // **************************
    // Set up
    // **************************
    static String CAMEL_SQL = "select * FROM ADDRESS";

    @Override
    protected void doPreSetup() throws Exception {
        try (Statement stmt = db.connection.createStatement()) {
            //stmt.executeUpdate("DROP TABLE ADDRESS");
            stmt.executeUpdate("CREATE TABLE ADDRESS (STREET VARCHAR(255), NUMMER INTEGER)");
            stmt.executeUpdate("INSERT INTO ADDRESS VALUES ('East Davie Street', 100)");
            stmt.executeUpdate("INSERT INTO ADDRESS VALUES ('Am Treptower Park', 75)");
            stmt.executeUpdate("INSERT INTO ADDRESS VALUES ('Werner-von-Siemens-Ring', 14)");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @After
    public void after() throws SQLException {
        try (Statement stmt = db.connection.createStatement()) {
            stmt.executeUpdate("DROP TABLE ADDRESS");
        }
    }

    @Override
    protected List<Step> createSteps() {
        return Arrays.asList(
            newSimpleEndpointStep(
                "direct",
                builder -> builder.putConfiguredProperty("name", "start")),
            newSqlEndpointStep(
                "sql-connector",
                builder -> builder.putConfiguredProperty("query", CAMEL_SQL)),
            newSimpleEndpointStep(
                "log",
                builder -> builder.putConfiguredProperty("loggerName", "test"))
        );
    }

    // **************************
    // Tests
    // **************************

    @Test
    public void sqlConnectorQueryTest() throws SQLException {
        List<?> results = template.requestBody("direct:start", null, List.class);

        SqlStatementParser parser = new SqlStatementParser(db.connection);
        SqlStatementMetaData md = parser.parse(CAMEL_SQL);
        
        Assertions.assertThat(results).hasSize(3);
        Properties rowEntry = JSONBeanUtil.parsePropertiesFromJSONBean(results.get(0).toString());
        
        Assertions.assertThat(rowEntry).hasSize(2);
        Assertions.assertThat(rowEntry.getProperty(md.getOutParams().get(0).getName())).isEqualTo("East Davie Street");
        Assertions.assertThat(rowEntry.getProperty(md.getOutParams().get(1).getName())).isEqualTo("100");
        
        rowEntry = JSONBeanUtil.parsePropertiesFromJSONBean(results.get(1).toString());
        Assertions.assertThat(rowEntry.getProperty(md.getOutParams().get(0).getName())).isEqualTo("Am Treptower Park");
        Assertions.assertThat(rowEntry.getProperty(md.getOutParams().get(1).getName())).isEqualTo("75");
        
        rowEntry = JSONBeanUtil.parsePropertiesFromJSONBean(results.get(2).toString());
        Assertions.assertThat(rowEntry.getProperty(md.getOutParams().get(0).getName())).isEqualTo("Werner-von-Siemens-Ring");
        Assertions.assertThat(rowEntry.getProperty(md.getOutParams().get(1).getName())).isEqualTo("14");

    }
    
}

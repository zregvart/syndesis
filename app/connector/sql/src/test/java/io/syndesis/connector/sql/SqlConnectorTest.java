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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.syndesis.connector.sql.common.Db;
import io.syndesis.connector.sql.common.DbAdapter;
import io.syndesis.connector.sql.common.JSONBeanUtil;
import io.syndesis.connector.sql.util.SqlConnectorTestSupport;
import io.syndesis.common.model.integration.Step;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

public class SqlConnectorTest extends SqlConnectorTestSupport {

    // **************************
    // Set up
    // **************************

    @Override
    protected void doPreSetup() throws Exception {
        Db testDb = new DbAdapter(db.connection).getDb();
        try (Statement stmt = db.connection.createStatement()) {
            String CREATION_SQL = "CREATE TABLE ADDRESS ("
                    + "ID " + testDb.getAutoIncrementGrammar() + ", "
                    + "street VARCHAR(255), "
                    + "nummer INTEGER)";
            try {
                stmt.executeUpdate(CREATION_SQL);
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
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
                builder -> builder.putConfiguredProperty("query", "INSERT INTO ADDRESS (street, nummer) VALUES (:#street, :#nummer)")),
            newSimpleEndpointStep(
                "log",
                builder -> builder.putConfiguredProperty("loggerName", "test"))
        );
    }

    // **************************
    // Tests
    // **************************

    @Test
    public void sqlConnectorTest() throws Exception {

        Map<String,Object> values = new HashMap<>();
        values.put("street", "LaborInVain");
        values.put("nummer", 14);
        String body = JSONBeanUtil.toJSONBean(values);

        String result = template.requestBody("direct:start", body, String.class);
        Assertions.assertThat(result).startsWith("[{\"GENERATEDKEY\":");

        try (Statement stmt = db.connection.createStatement()) {
            stmt.execute("SELECT * FROM ADDRESS");
            ResultSet resultSet = stmt.getResultSet();
            resultSet.next();
            //System.out.println(resultSet.getInt(1) + " " + resultSet.getString(2) + " " + resultSet.getInt(3));
            Assertions.assertThat(resultSet.getInt(1)).isBetween(1, 3);
            Assertions.assertThat(resultSet.getString(2)).isEqualTo("LaborInVain");
            Assertions.assertThat(resultSet.getInt(3)).isEqualTo(14);
        }
    }
}

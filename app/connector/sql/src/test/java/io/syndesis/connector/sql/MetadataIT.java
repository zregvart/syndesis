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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;

import io.syndesis.connector.sql.common.SqlParam;
import io.syndesis.connector.sql.common.SqlStatementMetaData;
import io.syndesis.connector.sql.common.SqlStatementParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class MetadataIT {

    @Rule
    public JdbcDatabaseContainer<?> database;

    public MetadataIT(final JdbcDatabaseContainer<?> database) {
        this.database = database;
    }

    @Test
    public void shouldFetchSqlMetadata() throws SQLException {
        try (Connection connection = database.createConnection(""); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE META_TEST (I INTEGER, C CHARACTER(3), VC VARCHAR(5), D DECIMAL(5,2))");

            final SqlStatementParser parser = new SqlStatementParser(connection);

            final SqlStatementMetaData metaData = parser.parse("SELECT * FROM META_TEST WHERE I = :pI OR C = :pC OR VC = :pVC OR D = :pD");

            assertThat(metaData.getInParams()).usingElementComparator(MetadataIT::fuzzyInParameters).containsOnly(
                new SqlParam("pI", Types.INTEGER),
                new SqlParam("pC", Types.CHAR),
                new SqlParam("pVC", Types.VARCHAR),
                new SqlParam("pD", Types.NUMERIC));

            assertThat(metaData.getOutParams()).usingElementComparator(MetadataIT::fuzzyOutParameters).containsOnly(
                new SqlParam("I", Types.INTEGER),
                new SqlParam("C", Types.CHAR),
                new SqlParam("VC", Types.VARCHAR),
                new SqlParam("D", Types.DECIMAL));
        }
    }

    @SuppressWarnings("resource")
    @Parameters
    public static Collection<Object> databaseContainerImages() {
        return Arrays.asList(new MySQLContainer() {
            @Override
            public String getDriverClassName() {
                return "com.mysql.cj.jdbc.Driver";
            }
        }, new PostgreSQLContainer<>(), new MariaDBContainer<>(), new DerbyContainer());
    }

    /**
     * In parameters can differentiate on the case of the parameter name, or if
     * we can't determine the parameter type then it's VARCHAR by default so
     * anything goes.
     */
    static int fuzzyInParameters(final SqlParam left, final SqlParam right) {
        final boolean nameEqualsKinda = left.getName().compareToIgnoreCase(right.getName()) == 0;

        // exact match
        final boolean typesAreEqual = left.getJdbcType() == right.getJdbcType();

        // at least one type is VARCHAR, some implementations return VARCHAR for
        // any type (looking at you MySql & MariaDB)
        final boolean isVarchar = left.getJdbcType() == Types.VARCHAR || right.getJdbcType() == Types.VARCHAR;

        // in the same class of numeric types
        final boolean typesAreNumeric = in(left.getJdbcType(), right.getJdbcType(), Types.NUMERIC, Types.DECIMAL);

        final boolean typeEqualsKinda = typesAreEqual || isVarchar || typesAreNumeric;

        if (nameEqualsKinda && typeEqualsKinda) {
            return 0;
        }

        final int nameCompared = left.getName().compareTo(right.getName());
        if (nameCompared != 0) {
            return nameCompared;
        }

        if (left.getJdbcType() < right.getJdbcType()) {
            return -1;
        }

        return 1;
    }

    /**
     * Out parameters can differentiate on the case of the parameter name.
     */
    static int fuzzyOutParameters(final SqlParam left, final SqlParam right) {
        if (left.getName().compareToIgnoreCase(right.getName()) == 0) {
            return 0;
        }

        return left.getName().compareTo(right.getName());
    }

    static boolean in(final int left, final int right, final int... kindaEqualTypes) {
        Arrays.sort(kindaEqualTypes);

        final int foundLeft = Arrays.binarySearch(kindaEqualTypes, left);
        final int foundRight = Arrays.binarySearch(kindaEqualTypes, right);

        return foundLeft >= 0 && foundRight >= 0;
    }
}

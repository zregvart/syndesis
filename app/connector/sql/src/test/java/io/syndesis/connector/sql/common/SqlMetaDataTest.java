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
package io.syndesis.connector.sql.common;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class SqlMetaDataTest {

    @ClassRule
    public static SqlConnectionRule db = new SqlConnectionRule();

    @Test
    public void defaultValuesTest() throws SQLException {
        try (Statement stmt = db.connection.createStatement()) {
            final String createTable = "CREATE TABLE ALLTYPES (charType CHAR, varcharType VARCHAR(255), "
                + "numericType NUMERIC, decimalType DECIMAL, smallintType SMALLINT," + "dateType DATE, timeType TIME )";
            stmt.executeUpdate(createTable);
            final String sql = String.format("INSERT INTO ALLTYPES VALUES ('J', 'Jackson',0, 0, 0, '%s', '%s')", SqlParam.SqlSampleValue.DATE_VALUE,
                SqlParam.SqlSampleValue.TIME_VALUE);
            stmt.executeUpdate(sql);
        }

        String select = "SELECT * FROM ALLTYPES where charType=:#myCharValue";
        final SqlStatementParser sqlParser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData paramInfo = sqlParser.parse(select);
        final List<SqlParam> inputParams = paramInfo.getInParams();
        // information for input
        Assert.assertEquals(1, inputParams.size());
        Assert.assertEquals(String.class, inputParams.get(0).getSampleValue().getClazz());

        // information for output of select statement
        final List<SqlParam> outputParams = paramInfo.getOutParams();
        Assert.assertEquals(7, outputParams.size());
        Assert.assertEquals(String.class, outputParams.get(0).getSampleValue().getClazz());
    }

    @Test
    public void parseInsertAllColumns() throws SQLException {
        try (Statement stmt = db.connection.createStatement()) {
            final String createTable = "CREATE TABLE NAME2 (id INTEGER PRIMARY KEY, firstName VARCHAR(255), " + "lastName VARCHAR(255))";
            stmt.executeUpdate(createTable);
            stmt.executeUpdate("INSERT INTO NAME2 VALUES (1, 'Joe', 'Jackson')");
            stmt.executeUpdate("INSERT INTO NAME2 VALUES (2, 'Roger', 'Waters')");
        }

        final String sqlStatement = "INSERT INTO NAME2 VALUES (:#id, :#first, :#last)";
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse(sqlStatement);

        Assert.assertEquals("INTEGER", JDBCType.valueOf(info.getInParams().get(0).getJdbcType()).getName());
        Assert.assertEquals("VARCHAR", JDBCType.valueOf(info.getInParams().get(1).getJdbcType()).getName());
        Assert.assertEquals("VARCHAR", JDBCType.valueOf(info.getInParams().get(2).getJdbcType()).getName());

    }

    @Test
    public void parseInsertWithSpecifiedColumnNames() throws SQLException {
        try (Statement stmt = db.connection.createStatement()) {
            final String createTable = "CREATE TABLE NAME3 (id INTEGER PRIMARY KEY, firstName VARCHAR(255), " + "lastName VARCHAR(255))";
            stmt.executeUpdate(createTable);
            stmt.executeUpdate("INSERT INTO NAME3 (ID, FIRSTNAME, LASTNAME) VALUES (1, 'Joe', 'Jackson')");
            stmt.executeUpdate("INSERT INTO NAME3 (ID, FIRSTNAME, LASTNAME) VALUES (2, 'Roger', 'Waters')");
        }

        final String sqlStatement = "INSERT INTO NAME3 (ID, FIRSTNAME, LASTNAME) VALUES (:#id, :#first, :#last)";
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse(sqlStatement);

        Assert.assertEquals("INTEGER", JDBCType.valueOf(info.getInParams().get(0).getJdbcType()).getName());
        Assert.assertEquals("VARCHAR", JDBCType.valueOf(info.getInParams().get(1).getJdbcType()).getName());
        Assert.assertEquals("VARCHAR", JDBCType.valueOf(info.getInParams().get(2).getJdbcType()).getName());

    }

    @Test
    public void parseInsertAutoIncrPK() throws SQLException {
        Db testDb = new DbAdapter(db.connection).getDb();
        try (Statement stmt = db.connection.createStatement()) {
            final String createTable = "CREATE TABLE NAME4 (id2 " + testDb.getAutoIncrementGrammar() + ", firstName VARCHAR(255), " + "lastName VARCHAR(255))";
            stmt.executeUpdate(createTable);
            stmt.executeUpdate("INSERT INTO NAME4 (FIRSTNAME, LASTNAME) VALUES ('Joe', 'Jackson')");
            stmt.executeUpdate("INSERT INTO NAME4 (FIRSTNAME, LASTNAME) VALUES ('Roger', 'Waters')");
        }

        final String sqlStatement = "INSERT INTO NAME4 (FIRSTNAME, LASTNAME) VALUES (:#first, :#last)";
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse(sqlStatement);

        Assert.assertEquals("VARCHAR", JDBCType.valueOf(info.getInParams().get(0).getJdbcType()).getName());
        Assert.assertEquals("VARCHAR", JDBCType.valueOf(info.getInParams().get(1).getJdbcType()).getName());

    }

    @Test
    public void parseSelect() throws SQLException {
        final Statement stmt = db.connection.createStatement();
        final String createTable = "CREATE TABLE NAME (id INTEGER PRIMARY KEY, firstName VARCHAR(255), " + "lastName VARCHAR(255))";
        stmt.executeUpdate(createTable);
        stmt.executeUpdate("INSERT INTO name VALUES (1, 'Joe', 'Jackson')");
        stmt.executeUpdate("INSERT INTO NAME VALUES (2, 'Roger', 'Waters')");

        final String sqlStatement = "SELECT FIRSTNAME, LASTNAME FROM NAME WHERE ID=:#id";
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse(sqlStatement);

        Assert.assertEquals("VARCHAR", JDBCType.valueOf(info.getOutParams().get(0).getJdbcType()).getName());
        Assert.assertEquals("VARCHAR", JDBCType.valueOf(info.getOutParams().get(1).getJdbcType()).getName());
    }

    @AfterClass
    public static void afterClass() throws SQLException {
        try (final Statement stmt = db.connection.createStatement()) {
            stmt.execute("DROP table ALLTYPES");
            stmt.execute("DROP table NAME");
            stmt.execute("DROP table NAME2");
            stmt.execute("DROP table NAME3");
            stmt.execute("DROP table NAME4");
        }
    }

}

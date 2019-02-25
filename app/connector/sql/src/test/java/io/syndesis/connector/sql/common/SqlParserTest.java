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

import java.math.BigDecimal;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class SqlParserTest {

    @ClassRule
    public static SqlConnectionRule db = new SqlConnectionRule();

    @Test
    public void parseUpdateWithConstant() throws SQLException {
        SqlStatementParser parser = new SqlStatementParser(db.connection);
        SqlStatementMetaData info = parser.parse("UPDATE NAME0 SET FIRSTNAME=:#first, LASTNAME='Jenssen' "
                + "WHERE ID=:#id");
        Assert.assertEquals(2, info.getInParams().size());
        Assert.assertEquals("first", info.getInParams().get(0).getName());
        Assert.assertEquals(String.class, info.getInParams().get(0).getSampleValue().getClazz());
        Assert.assertEquals("id", info.getInParams().get(1).getName());
        if (db.connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("ORACLE")) {
            Assert.assertEquals(BigDecimal.class, info.getInParams().get(1).getSampleValue().getClazz());
        } else {
            Assert.assertEquals(Integer.class, info.getInParams().get(1).getSampleValue().getClazz());
        }
    }

    @Test
    public void parseDelete() throws SQLException {
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse("DELETE FROM NAME0 WHERE ID=:#id");
        Assert.assertEquals(1, info.getInParams().size());
        Assert.assertEquals("id", info.getInParams().get(0).getName());
        if (db.connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("ORACLE")) {
            Assert.assertEquals(BigDecimal.class, info.getInParams().get(0).getSampleValue().getClazz());
        } else {
            Assert.assertEquals(Integer.class, info.getInParams().get(0).getSampleValue().getClazz());
        }
    }

    @Test
    public void parseInsertIntoAllColumnsOfTheTable() throws SQLException {
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse("INSERT INTO NAME0 VALUES (:#id, :#firstname, :#lastname)");
        Assert.assertEquals(3, info.getInParams().size());
        Assert.assertEquals("id", info.getInParams().get(0).getName());
        Assert.assertEquals("firstname", info.getInParams().get(1).getName());
        Assert.assertEquals("lastname", info.getInParams().get(2).getName());
        Assert.assertEquals(String.class, info.getInParams().get(2).getSampleValue().getClazz());
    }

    @Test
    public void parseInsertWithConstant() throws SQLException {
        SqlStatementParser parser = new SqlStatementParser(db.connection);
        SqlStatementMetaData info = parser.parse("INSERT INTO NAME0 VALUES (29, :#firstname, :#lastname)");
        Assert.assertEquals(2, info.getInParams().size());
        Assert.assertEquals("firstname", info.getInParams().get(0).getName());
        Assert.assertEquals("lastname", info.getInParams().get(1).getName());
        Assert.assertEquals(String.class, info.getInParams().get(1).getSampleValue().getClazz());
    }

    @Test
    public void parseInsertWithConstantLowerCase() throws SQLException {
        SqlStatementParser parser = new SqlStatementParser(db.connection);
        SqlStatementMetaData info = parser.parse("INSERT INTO NAME0 values (29, :#firstname, :#lastname)");
        Assert.assertEquals(2, info.getInParams().size());
        Assert.assertEquals("firstname", info.getInParams().get(0).getName());
        Assert.assertEquals("lastname", info.getInParams().get(1).getName());
        Assert.assertEquals(String.class, info.getInParams().get(1).getSampleValue().getClazz());
    }
    
    @Test
    public void parseInsertWithSpecifiedColumnNames() throws SQLException {
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse(
                "INSERT INTO NAME0 (ID, FIRSTNAME, LASTNAME) VALUES (1,:#firstname, :#lastname)");
        Assert.assertEquals(2, info.getInParams().size());
        Assert.assertEquals("firstname", info.getInParams().get(0).getName());
        Assert.assertEquals("lastname", info.getInParams().get(1).getName());
    }

    @Test
    public void parseSelect() throws SQLException {
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse("SELECT FIRSTNAME, LASTNAME FROM NAME0 WHERE ID=:#id");
        Assert.assertEquals(1, info.getInParams().size());
        Assert.assertEquals("id", info.getInParams().get(0).getName());
    }

    @Test
    public void parseSelectWithJoin() throws SQLException {
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse(
            "SELECT FIRSTNAME, NAME0.LASTNAME, ADDRESS FROM NAME0, ADDRESS0 WHERE "
            + "NAME0.LASTNAME=ADDRESS0.LASTNAME AND FIRSTNAME LIKE :#first");
        Assert.assertEquals(1, info.getInParams().size());
        Assert.assertEquals("first", info.getInParams().get(0).getName());
    }

    @Test
    public void parseSelectWithLike() throws SQLException {
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse("SELECT FIRSTNAME, LASTNAME FROM NAME0 WHERE FIRSTNAME LIKE :#first");
        Assert.assertEquals(1, info.getInParams().size());
        Assert.assertEquals("first", info.getInParams().get(0).getName());
    }

    @Test
    public void parseUpdate() throws SQLException {
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        final SqlStatementMetaData info = parser.parse("UPDATE NAME0 SET FIRSTNAME=:#first WHERE ID=:#id");
        Assert.assertEquals(2, info.getInParams().size());
        Assert.assertEquals("first", info.getInParams().get(0).getName());
        Assert.assertEquals("id", info.getInParams().get(1).getName());
    }

    @Test
    public void parseInsertWithConstantAndColumnNames() throws SQLException {
        SqlStatementParser parser = new SqlStatementParser(db.connection);
        SqlStatementMetaData info = parser.parse("INSERT INTO NAME0 (ID, FIRSTNAME, LASTNAME) VALUES (1, 'Kurt', :#lastname)");
        Assert.assertEquals(1, info.getInParams().size());
        Assert.assertEquals("lastname", info.getInParams().get(0).getName());
    }
    
    @Test(expected=SQLException.class)
    public void parseSelectFromNoneExistingTable() throws SQLException {
        final SqlStatementParser parser = new SqlStatementParser(db.connection);
        parser.parse("SELECT FIRSTNAME, LASTNAME FROM NAME-NOTEXIST WHERE FIRSTNAME LIKE :#first");
    }
}

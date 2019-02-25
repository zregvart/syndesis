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

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

public class SqlStatementParser {

    /*
     * C - INSERT INTO NAME VALUES (:id, :firstname, :lastname)
     * R - SELECT FIRSTNAME, LASTNAME FROM NAME WHERE ID=:id
     * U - UPDATE NAME SET FIRSTNAME=:firstname WHERE ID=:id
     * D - DELETE FROM NAME WHERE ID=:id
     *
     * DEMO_ADD(INTEGER ${body[A]}
     *
     * validate no "AS"
     * input params
     * output params
     * table name
     */
    private final Connection connection;
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlStatementParser.class);

    public SqlStatementParser(Connection connection) throws SQLException {
        this.connection = connection;
    }

    public SqlStatementMetaData parse(String camelSql) throws SQLException {

        SqlStatementMetaData statementInfo = new SqlStatementMetaData(camelSql.trim());
        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(statementInfo.getSqlStatement());
        List<SqlParameter> params = NamedParameterUtils.buildSqlParameterList(parsedSql, new MapSqlParameterSource());
        String sql = NamedParameterUtils.parseSqlStatementIntoString(statementInfo.getSqlStatement());

        PreparedStatement prepStmt = null;
        try {

            //Set InParams
            prepStmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statementInfo.setInParams(findInputParamsInPrepStmt(prepStmt, params));

            //Set OutParams
            if (sql.toUpperCase(Locale.US).startsWith("SELECT")) {
                statementInfo.setOutParams(findOutputColumnsInSelect(prepStmt, statementInfo.getInParams()));
            }
            if (sql.toUpperCase(Locale.US).startsWith("INSERT")) {
                boolean isAutoCommit = connection.getAutoCommit();
                try {
                    connection.setAutoCommit(false);
                    statementInfo.setOutParams(findGeneratedKeyColumnInInsert(prepStmt, statementInfo.getInParams()));
                    if (statementInfo.getOutParams().size()>0) {
                        statementInfo.setHasGeneratedKeys(true);
                    }
                } finally {
                    try {
                        connection.rollback();
                    } catch (Exception e) {
                        LOGGER.warn(e.getMessage());
                    }
                    connection.setAutoCommit(isAutoCommit);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            if (prepStmt != null) {
                prepStmt.close();
            }
        }
        return statementInfo;
    }

    private List<SqlParam> findInputParamsInPrepStmt(PreparedStatement prepStmt, List<SqlParameter> params) throws SQLException {
        List<SqlParam> list = new ArrayList<>();
        if (!params.isEmpty()) {
            for (int i=0; i<params.size();i++) {
                String name = params.get(i).getName();
                if (name.startsWith("#")) {
                    name = name.substring(1);
                }
                ParameterMetaData md = prepStmt.getParameterMetaData();
                try {
                    int jdbcType = prepStmt.getParameterMetaData().getParameterType(i+1);
                    list.add(new SqlParam(name, jdbcType));
                } catch (SQLException ignored) {
                    list.add(new SqlParam(name, Types.VARCHAR));
                }
            }
        }
        return list;
    }

    private List<SqlParam> findOutputColumnsInSelect(PreparedStatement prepStmt, List<SqlParam> inParams) throws SQLException {
        List<SqlParam> list = new ArrayList<>();
        ResultSetMetaData md = prepStmt.getMetaData();
        for (int column=1; column<=md.getColumnCount(); column++) {
            list.add(new SqlParam(
                    md.getColumnName(column),
                    md.getColumnType(column)));
        }
        return list;
    }

    private List<SqlParam> findGeneratedKeyColumnInInsert(PreparedStatement prepStmt, List<SqlParam> inParams) throws SQLException {
        List<SqlParam> list = new ArrayList<>();
        if (prepStmt.executeUpdate() > 0 ) {
            ResultSet rs = prepStmt.getGeneratedKeys();
            if (rs != null) {
                ResultSetMetaData md = rs.getMetaData();
                list.add(new SqlParam(
                        "GENERATEDKEY",
                        md.getColumnType(1)));
            }
        }
        return list;
    }
}

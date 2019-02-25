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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class DbMetaDataHelper {

    Db db;
    DatabaseMetaData meta;

    public DbMetaDataHelper(final Connection connection) throws SQLException {
        this.db = new DbAdapter(connection).getDb();
        this.meta = connection.getMetaData();
    }

    public String getDefaultSchema(final String dbUser) {
        return db.getDefaultSchema(dbUser);
    }

    public String adapt(final String pattern) {
        return db.adaptPattern(pattern);
    }

    public ResultSet fetchProcedureColumns(final String catalog,
        final String schema, final String procedureName) throws SQLException {
        return db.fetchProcedureColumns(meta, catalog, schema, procedureName);
    }

    public ResultSet fetchProcedures(final String catalog,
        final String schemaPattern, final String procedurePattern) throws SQLException {
        return db.fetchProcedures(meta, catalog, schemaPattern, procedurePattern);
    }

}

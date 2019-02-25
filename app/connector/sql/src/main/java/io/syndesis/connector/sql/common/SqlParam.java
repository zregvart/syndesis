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
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Data;

@Data
//@SuppressWarnings("PMD.StdCyclomaticComplexity")
public class SqlParam {

    private String name;
    int jdbcType;
    private SampleValue<?> sampleValue;

    public SqlParam() {
        super();
    }

    public SqlParam(String name, int jdbcType) {
        super();
        this.name = name;
        this.jdbcType = jdbcType;
        this.sampleValue = valueForType(jdbcType);
    }

    public void setJdbcType(int jdbcType) {
        this.jdbcType = jdbcType;
        this.sampleValue = valueForType(jdbcType);
    }


    @Data
    public static class SampleValue<T> {

        private Class<T> clazz;
        private T value;

        public SampleValue(Class<T> clazz, T value) {
            super();
            this.clazz = clazz;
            this.value = value;
        }
    }

    public static class SqlSampleValue {
        public static final List<String> ARRAY_VALUE = Collections.unmodifiableList(Arrays.asList("1","2","3"));
        public static final byte[] BINARY_VALUE = {1,2,3};
        public static final String STRING_VALUE = "abc";
        public static final String CHAR_VALUE = "a";
        public static final Date DATE_VALUE = new Date(new java.util.Date().getTime());
        public static final Time TIME_VALUE = new Time(new java.util.Date().getTime());
        public static final Timestamp TIMESTAMP_VALUE = new Timestamp(new java.util.Date().getTime());
        public static final BigDecimal DECIMAL_VALUE = BigDecimal.ZERO;
        public static final Boolean BOOLEAN_VALUE = Boolean.TRUE;
        public static final Double DOUBLE_VALUE = Double.valueOf(0);
        public static final Integer INTEGER_VALUE = 0;
        public static final Long LONG_VALUE = 0L;
        public static final Float FLOAT_VALUE = 0f;
    }

    //@SuppressWarnings({"rawtypes", "PMD.CyclomaticComplexity"})
    private static SampleValue<?> valueForType(final int jdbcType) {

        switch (jdbcType) {
        case Types.ARRAY:
        case Types.BINARY:
        case Types.BLOB:
        case Types.LONGVARBINARY:
        case Types.VARBINARY:
            return new SampleValue<>(List.class, SqlSampleValue.ARRAY_VALUE);
        case Types.BIT:
        case Types.BOOLEAN:
            return new SampleValue<>(Boolean.class, SqlSampleValue.BOOLEAN_VALUE);
        case Types.CHAR:
            return new SampleValue<>(String.class, SqlSampleValue.CHAR_VALUE);
        case Types.CLOB:
        case Types.DATALINK:
        case Types.LONGNVARCHAR:
        case Types.LONGVARCHAR:
        case Types.NCHAR:
        case Types.NCLOB:
        case Types.NVARCHAR:
        case Types.ROWID:
        case Types.SQLXML:
        case Types.VARCHAR:
            return new SampleValue<>(String.class, SqlSampleValue.STRING_VALUE);
        case Types.DATE:
            return new SampleValue<>(Date.class, SqlSampleValue.DATE_VALUE);
        case Types.TIME:
            return new SampleValue<>(Time.class, SqlSampleValue.TIME_VALUE);
        case Types.TIMESTAMP:
        case Types.TIMESTAMP_WITH_TIMEZONE:
        case Types.TIME_WITH_TIMEZONE:
            return new SampleValue<>(Timestamp.class, SqlSampleValue.TIMESTAMP_VALUE);
        case Types.DECIMAL:
        case Types.NUMERIC:
            return new SampleValue<>(BigDecimal.class, SqlSampleValue.DECIMAL_VALUE);
        case Types.FLOAT:
        case Types.DOUBLE:
            return new SampleValue<>(Double.class, SqlSampleValue.DOUBLE_VALUE);
        case Types.REAL:
            return new SampleValue<>(Float.class, SqlSampleValue.FLOAT_VALUE);
        case Types.BIGINT:
            return new SampleValue<>(Long.class, SqlSampleValue.LONG_VALUE);
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.TINYINT:
            return new SampleValue<>(Integer.class, SqlSampleValue.INTEGER_VALUE);
        case Types.NULL:
            return null;
        case Types.DISTINCT:
        case Types.JAVA_OBJECT:
        case Types.OTHER:
        case Types.REF:
        case Types.REF_CURSOR:
        case Types.STRUCT:
        default:
            return new SampleValue<>(String.class, SqlSampleValue.STRING_VALUE);
        }
    }

}

/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jirban.jira.impl.board;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.function.Function;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;
import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.osgi.framework.BundleReference;

/**
 * @author Kabir Khan
 */
public class RawSqlLoader {

    private final String dataSourceName;

    private RawSqlLoader(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public static RawSqlLoader create(String dataSourceName) {
        final ClassLoader cl = RawSqlLoader.class.getClassLoader();
        if (cl instanceof BundleReference) {
            return new RawSqlLoader(dataSourceName);
        }
        return null;
    }

    public ModelNode executeQuery(String sql) {
        if (true) {
            throw new JirbanValidationException("Db Explorer disabled");
        }
        
        if (!sql.contains("limit")) {
            //Limit the numbers, we don't want to crash the production instance while nosing around
            sql += " limit 100";
        }
        return internalExecute(sql, rs -> {
            try {
                final ModelNode result = new ModelNode();

                ModelNode header = new ModelNode().setEmptyList();
                final ResultSetMetaData meta = rs.getMetaData();
                final int cols = meta.getColumnCount();

                //For some obscure reason result set is 1-indexed
                for (int i = 1 ; i <= cols ; i++) {
                    header.add(meta.getColumnName(i));
                    meta.getColumnType(i);
                }
                result.get("headers").set(header);

                ModelNode rows = new ModelNode().setEmptyList();
                while (rs.next()) {
                    ModelNode row = new ModelNode().setEmptyList();
                    //For some obscure reason result set is 1-indexed
                    for (int i = 1 ; i <= cols ; i++) {
                        switch (meta.getColumnType(i)) {
                            case Types.BOOLEAN:
                            case Types.DECIMAL:
                            case Types.BIT:
                            case Types.SMALLINT:
                            case Types.TINYINT:
                            case Types.INTEGER: {
                                int val = rs.getInt(i);
                                if (rs.wasNull()) {
                                    row.add(new ModelNode());
                                } else {
                                    row.add(val);
                                }
                                break;
                            }
                            case Types.BIGINT: {
                                long val = rs.getLong(i);
                                if (rs.wasNull()) {
                                    row.add(new ModelNode());
                                } else {
                                    row.add(val);
                                }
                                break;
                            }
                            case Types.VARCHAR:
                            case Types.NVARCHAR: {
                                String val = rs.getString(i);
                                if (rs.wasNull()) {
                                    row.add(new ModelNode());
                                } else {
                                    row.add(val);
                                }
                                break;
                            }
                            default: {
                                rs.getObject(i);
                                if (rs.wasNull()) {
                                    row.add(new ModelNode());
                                } else {
                                    row.add("N/A (" + meta.getColumnType(i) + ")");
                                }
                            }
                        }
                    }
                    rows.add(row);
                }
                result.get("rows").set(rows);
                return result;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <R> R internalExecute(String sql, Function<ResultSet, R> function) {
        final SQLProcessor sqlProcessor = new SQLProcessor(dataSourceName);
        try {
            try (final ResultSet rs = sqlProcessor.executeQuery(sql)){
                return function.apply(rs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            safeClose(sqlProcessor);
        }
    }

    private void safeClose(SQLProcessor processor) {
        try {
            processor.close();
        } catch (Exception ignore) {

        }
    }
}

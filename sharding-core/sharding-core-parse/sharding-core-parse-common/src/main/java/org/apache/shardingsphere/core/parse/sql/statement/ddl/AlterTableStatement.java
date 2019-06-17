/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.parse.sql.statement.ddl;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.shardingsphere.core.metadata.table.ColumnMetaData;
import org.apache.shardingsphere.core.metadata.table.ShardingTableMetaData;
import org.apache.shardingsphere.core.parse.sql.segment.ddl.column.ColumnDefinitionSegment;
import org.apache.shardingsphere.core.parse.sql.segment.ddl.column.position.ColumnPositionSegment;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

/**
 * Alter table statement.
 * 
 * @author duhongjun
 */
@Getter
@Setter
@ToString(callSuper = true)
public final class AlterTableStatement extends DDLStatement {
    
    private final Collection<ColumnDefinitionSegment> addedColumnDefinitions = new LinkedList<>();
    
    private final Map<String, ColumnDefinitionSegment> modifiedColumnDefinitions = new LinkedHashMap<>();
    
    private final Collection<ColumnPositionSegment> changedPositionColumns = new TreeSet<>();
    
    private final Collection<String> droppedColumnNames = new LinkedList<>();
    
    private boolean dropPrimaryKey;
    
    /**
     * Find column definition.
     *
     * @param columnName column name
     * @param shardingTableMetaData sharding table meta data
     * @return column definition
     */
    public Optional<ColumnDefinitionSegment> findColumnDefinition(final String columnName, final ShardingTableMetaData shardingTableMetaData) {
        Optional<ColumnDefinitionSegment> result = findColumnDefinitionFromMetaData(columnName, shardingTableMetaData);
        return result.isPresent() ? result : findColumnDefinitionFromCurrentAddClause(columnName);
    }
    
    /**
     * Find column definition from meta data.
     *
     * @param columnName column name
     * @param shardingTableMetaData sharding table meta data
     * @return column definition
     */
    public Optional<ColumnDefinitionSegment> findColumnDefinitionFromMetaData(final String columnName, final ShardingTableMetaData shardingTableMetaData) {
        if (!shardingTableMetaData.containsTable(getTables().getSingleTableName())) {
            return Optional.absent();
        }
        for (ColumnMetaData each : shardingTableMetaData.get(getTables().getSingleTableName()).getColumns().values()) {
            if (columnName.equalsIgnoreCase(each.getColumnName())) {
                return Optional.of(new ColumnDefinitionSegment(columnName, each.getDataType(), each.isPrimaryKey()));
            }
        }
        return Optional.absent();
    }
    
    private Optional<ColumnDefinitionSegment> findColumnDefinitionFromCurrentAddClause(final String columnName) {
        for (ColumnDefinitionSegment each : addedColumnDefinitions) {
            if (each.getColumnName().equalsIgnoreCase(columnName)) {
                return Optional.of(each);
            }
        }
        return Optional.absent();
    }
}

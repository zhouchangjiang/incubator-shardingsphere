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

package org.apache.shardingsphere.core.route.router.sharding;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.core.metadata.datasource.ShardingDataSourceMetaData;
import org.apache.shardingsphere.core.optimize.result.OptimizeResult;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dal.DALStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dal.dialect.mysql.ShowDatabasesStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dal.dialect.mysql.UseStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dal.dialect.postgresql.ResetParameterStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dal.dialect.postgresql.SetStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dcl.DCLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.ddl.DDLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.parse.sql.statement.tcl.TCLStatement;
import org.apache.shardingsphere.core.route.type.RoutingEngine;
import org.apache.shardingsphere.core.route.type.broadcast.DataSourceGroupBroadcastRoutingEngine;
import org.apache.shardingsphere.core.route.type.broadcast.DatabaseBroadcastRoutingEngine;
import org.apache.shardingsphere.core.route.type.broadcast.InstanceBroadcastRoutingEngine;
import org.apache.shardingsphere.core.route.type.broadcast.TableBroadcastRoutingEngine;
import org.apache.shardingsphere.core.route.type.complex.ComplexRoutingEngine;
import org.apache.shardingsphere.core.route.type.defaultdb.DefaultDatabaseRoutingEngine;
import org.apache.shardingsphere.core.route.type.ignore.IgnoreRoutingEngine;
import org.apache.shardingsphere.core.route.type.standard.StandardRoutingEngine;
import org.apache.shardingsphere.core.route.type.unicast.UnicastRoutingEngine;
import org.apache.shardingsphere.core.rule.ShardingRule;

import java.util.Collection;

/**
 * Routing engine factory.
 *
 * @author zhangliang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoutingEngineFactory {
    
    /**
     * Create new instance of routing engine.
     * 
     * @param shardingRule sharding rule
     * @param shardingDataSourceMetaData sharding data source meta data
     * @param sqlStatement sql statement
     * @param optimizeResult optimize result
     * @return new instance of routing engine
     */
    public static RoutingEngine newInstance(final ShardingRule shardingRule,
                                            final ShardingDataSourceMetaData shardingDataSourceMetaData, final SQLStatement sqlStatement, final OptimizeResult optimizeResult) {
        Collection<String> tableNames = sqlStatement.getTables().getTableNames();
        if (sqlStatement instanceof TCLStatement) {
            return new DatabaseBroadcastRoutingEngine(shardingRule);
        }
        if (sqlStatement instanceof DDLStatement) {
            return new TableBroadcastRoutingEngine(shardingRule, sqlStatement, optimizeResult);
        }
        if (sqlStatement instanceof DALStatement) {
            return getDALRoutingEngine(shardingRule, sqlStatement, tableNames);
        }
        if (sqlStatement instanceof DCLStatement) {
            return getDCLRoutingEngine(shardingRule, sqlStatement, optimizeResult, shardingDataSourceMetaData);
        }
        if (shardingRule.isAllInDefaultDataSource(tableNames)) {
            return new DefaultDatabaseRoutingEngine(shardingRule, tableNames);
        }
        if (shardingRule.isAllBroadcastTables(tableNames)) {
            return sqlStatement instanceof SelectStatement ? new UnicastRoutingEngine(shardingRule, tableNames) : new DatabaseBroadcastRoutingEngine(shardingRule);
        }
        if (optimizeResult.getShardingConditions().isAlwaysFalse() || tableNames.isEmpty()) {
            return new UnicastRoutingEngine(shardingRule, tableNames);
        }
        Collection<String> shardingTableNames = shardingRule.getShardingLogicTableNames(tableNames);
        if (1 == shardingTableNames.size() || shardingRule.isAllBindingTables(shardingTableNames)) {
            return new StandardRoutingEngine(sqlStatement, shardingRule, shardingTableNames.iterator().next(), optimizeResult);
        }
        // TODO config for cartesian set
        return new ComplexRoutingEngine(sqlStatement, shardingRule, tableNames, optimizeResult);
    }
    
    private static RoutingEngine getDALRoutingEngine(final ShardingRule shardingRule, final SQLStatement sqlStatement, final Collection<String> tableNames) {
        if (sqlStatement instanceof ShowDatabasesStatement || sqlStatement instanceof UseStatement) {
            return new IgnoreRoutingEngine();
        }
        if (sqlStatement instanceof SetStatement || sqlStatement instanceof ResetParameterStatement) {
            return new DatabaseBroadcastRoutingEngine(shardingRule);
        }
        if (!tableNames.isEmpty()) {
            return new UnicastRoutingEngine(shardingRule, tableNames);
        }
        return new DataSourceGroupBroadcastRoutingEngine(shardingRule);
    }
    
    private static RoutingEngine getDCLRoutingEngine(final ShardingRule shardingRule, 
                                                     final SQLStatement sqlStatement, final OptimizeResult optimizeResult, final ShardingDataSourceMetaData shardingDataSourceMetaData) {
        return isGrantForSingleTable(sqlStatement) 
                ? new TableBroadcastRoutingEngine(shardingRule, sqlStatement, optimizeResult) : new InstanceBroadcastRoutingEngine(shardingRule, shardingDataSourceMetaData);
    }
    
    private static boolean isGrantForSingleTable(final SQLStatement sqlStatement) {
        return !sqlStatement.getTables().isEmpty() && !"*".equals(sqlStatement.getTables().getSingleTableName());
    }
}

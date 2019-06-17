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

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.parse.SQLParseEngine;
import org.apache.shardingsphere.core.parse.rule.registry.MasterSlaveParseRuleRegistry;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.route.SQLRouteResult;
import org.apache.shardingsphere.core.route.type.RoutingResult;
import org.apache.shardingsphere.core.route.type.hint.DatabaseHintRoutingEngine;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.core.strategy.route.hint.HintShardingStrategy;
import org.apache.shardingsphere.spi.database.DatabaseType;

import java.util.List;

/**
 * Sharding router for hint database only.
 * 
 * @author zhangliang
 * @author maxiaoguang
 */
// TODO removed after all ANTLR parser finished
@RequiredArgsConstructor
public final class DatabaseHintSQLRouter implements ShardingRouter {
    
    private final DatabaseType databaseType;
    
    private final ShardingRule shardingRule;
    
    @Override
    public SQLStatement parse(final String logicSQL, final boolean useCache) {
        return new SQLParseEngine(MasterSlaveParseRuleRegistry.getInstance(), databaseType, logicSQL, null, null).parse();
    }
    
    @Override
    // TODO insert SQL need parse gen key
    public SQLRouteResult route(final SQLStatement sqlStatement, final List<Object> parameters) {
        SQLRouteResult result = new SQLRouteResult(sqlStatement);
        RoutingResult routingResult = new DatabaseHintRoutingEngine(
                shardingRule.getShardingDataSourceNames().getDataSourceNames(), (HintShardingStrategy) shardingRule.getDefaultDatabaseShardingStrategy()).route();
        result.setRoutingResult(routingResult);
        return result;
    }
}

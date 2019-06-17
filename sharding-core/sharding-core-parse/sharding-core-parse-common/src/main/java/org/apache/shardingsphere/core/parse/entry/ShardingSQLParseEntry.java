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

package org.apache.shardingsphere.core.parse.entry;

import org.apache.shardingsphere.core.metadata.table.ShardingTableMetaData;
import org.apache.shardingsphere.core.parse.SQLParseEngine;
import org.apache.shardingsphere.core.parse.cache.ParsingResultCache;
import org.apache.shardingsphere.core.parse.rule.registry.ShardingParseRuleRegistry;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.spi.database.DatabaseType;

/**
 * SQL parse entry for sharding.
 *
 * @author zhangliang
 */
public final class ShardingSQLParseEntry extends SQLParseEntry {
    
    private final DatabaseType databaseType;
    
    private final ShardingRule shardingRule;
    
    private final ShardingTableMetaData shardingTableMetaData;
    
    public ShardingSQLParseEntry(final DatabaseType databaseType, final ShardingRule shardingRule, final ShardingTableMetaData shardingTableMetaData, final ParsingResultCache parsingResultCache) {
        super(parsingResultCache);
        this.databaseType = databaseType;
        this.shardingRule = shardingRule;
        this.shardingTableMetaData = shardingTableMetaData;
    }
    
    @Override
    protected SQLParseEngine getSQLParseEngine(final String sql) {
        return new SQLParseEngine(ShardingParseRuleRegistry.getInstance(), databaseType, sql, shardingRule, shardingTableMetaData);
    }
}

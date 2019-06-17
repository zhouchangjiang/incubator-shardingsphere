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

package org.apache.shardingsphere.core.parse.rule.registry;

import org.apache.shardingsphere.core.database.DatabaseTypes;
import org.apache.shardingsphere.core.parse.sql.segment.ddl.column.ColumnDefinitionSegment;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class ParseRuleRegistryTest {
    
    private static ParseRuleRegistry shardingParseRuleRegistry = ShardingParseRuleRegistry.getInstance();
    
    private static ParseRuleRegistry encryptParseRuleRegistry = EncryptParseRuleRegistry.getInstance();
    
    private static ParseRuleRegistry masterSlaveParseRuleRegistry = MasterSlaveParseRuleRegistry.getInstance();
    
    @Test
    public void assertShardingParseRuleRegistry() {
        assertNotNull(shardingParseRuleRegistry.getSQLStatementRule(DatabaseTypes.getActualDatabaseType("MySQL"), "SelectContext"));
        assertTrue(shardingParseRuleRegistry.findSQLSegmentFiller(DatabaseTypes.getActualDatabaseType("MySQL"), ColumnDefinitionSegment.class).isPresent());
    }
    
    @Test
    public void assertEncryptParseRuleRegistry() {
        assertNotNull(encryptParseRuleRegistry.getSQLStatementRule(DatabaseTypes.getActualDatabaseType("MySQL"), "SelectContext"));
        assertTrue(encryptParseRuleRegistry.findSQLSegmentFiller(DatabaseTypes.getActualDatabaseType("MySQL"), ColumnDefinitionSegment.class).isPresent());
    }
    
    @Test
    public void assertMasterSlaveParseRuleRegistry() {
        assertNotNull(masterSlaveParseRuleRegistry.getSQLStatementRule(DatabaseTypes.getActualDatabaseType("MySQL"), "SelectContext"));
        assertTrue(masterSlaveParseRuleRegistry.findSQLSegmentFiller(DatabaseTypes.getActualDatabaseType("MySQL"), ColumnDefinitionSegment.class).isPresent());
    }
}

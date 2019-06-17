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

package org.apache.shardingsphere.shardingproxy.backend.schema;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.core.metadata.ShardingMetaData;
import org.apache.shardingsphere.core.metadata.datasource.ShardingDataSourceMetaData;
import org.apache.shardingsphere.core.metadata.table.ShardingTableMetaData;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.ddl.AlterTableStatement;
import org.apache.shardingsphere.core.parse.sql.statement.ddl.CreateIndexStatement;
import org.apache.shardingsphere.core.parse.sql.statement.ddl.CreateTableStatement;
import org.apache.shardingsphere.core.parse.sql.statement.ddl.DropIndexStatement;
import org.apache.shardingsphere.core.parse.sql.statement.ddl.DropTableStatement;
import org.apache.shardingsphere.core.rule.MasterSlaveRule;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.orchestration.internal.registry.config.event.ShardingRuleChangedEvent;
import org.apache.shardingsphere.orchestration.internal.registry.state.event.DisabledStateChangedEvent;
import org.apache.shardingsphere.orchestration.internal.registry.state.schema.OrchestrationShardingSchema;
import org.apache.shardingsphere.orchestration.internal.rule.OrchestrationMasterSlaveRule;
import org.apache.shardingsphere.orchestration.internal.rule.OrchestrationShardingRule;
import org.apache.shardingsphere.shardingproxy.config.yaml.YamlDataSourceParameter;

import java.util.Collection;
import java.util.Map;

/**
 * Sharding schema.
 *
 * @author zhangliang
 * @author zhangyonglun
 * @author panjuan
 * @author zhaojun
 * @author wangkai
 */
@Getter
public final class ShardingSchema extends LogicSchema {
    
    private ShardingRule shardingRule;
    
    private final ShardingMetaData metaData;
    
    public ShardingSchema(final String name, final Map<String, YamlDataSourceParameter> dataSources, final ShardingRuleConfiguration shardingRuleConfig, final boolean isUsingRegistry) {
        super(name, dataSources);
        shardingRule = createShardingRule(shardingRuleConfig, dataSources.keySet(), isUsingRegistry);
        metaData = createShardingMetaData();
    }
    
    private ShardingRule createShardingRule(final ShardingRuleConfiguration shardingRuleConfig, final Collection<String> dataSourceNames, final boolean isUsingRegistry) {
        return isUsingRegistry ? new OrchestrationShardingRule(shardingRuleConfig, dataSourceNames) : new ShardingRule(shardingRuleConfig, dataSourceNames);
    }
    
    private ShardingMetaData createShardingMetaData() {
        ShardingDataSourceMetaData shardingDataSourceMetaData = new ShardingDataSourceMetaData(getDataSourceURLs(getDataSources()), shardingRule, LogicSchemas.getInstance().getDatabaseType());
        ShardingTableMetaData shardingTableMetaData = new ShardingTableMetaData(getTableMetaDataInitializer(shardingDataSourceMetaData).load(shardingRule));
        return new ShardingMetaData(shardingDataSourceMetaData, shardingTableMetaData);
    }
    
    /**
     * Renew sharding rule.
     *
     * @param shardingRuleChangedEvent sharding rule changed event.
     */
    @Subscribe
    public synchronized void renew(final ShardingRuleChangedEvent shardingRuleChangedEvent) {
        if (getName().equals(shardingRuleChangedEvent.getShardingSchemaName())) {
            shardingRule = new OrchestrationShardingRule(shardingRuleChangedEvent.getShardingRuleConfiguration(), getDataSources().keySet());
        }
    }
    
    /**
     * Renew disabled data source names.
     *
     * @param disabledStateChangedEvent disabled state changed event
     */
    @Subscribe
    public synchronized void renew(final DisabledStateChangedEvent disabledStateChangedEvent) {
        OrchestrationShardingSchema shardingSchema = disabledStateChangedEvent.getShardingSchema();
        if (getName().equals(shardingSchema.getSchemaName())) {
            for (MasterSlaveRule each : shardingRule.getMasterSlaveRules()) {
                ((OrchestrationMasterSlaveRule) each).updateDisabledDataSourceNames(shardingSchema.getDataSourceName(), disabledStateChangedEvent.isDisabled());
            }
        }
    }
    
    @Override
    public void refreshTableMetaData(final SQLStatement sqlStatement) {
        if (sqlStatement instanceof CreateTableStatement) {
            refreshTableMetaData((CreateTableStatement) sqlStatement);
        } else if (sqlStatement instanceof AlterTableStatement) {
            refreshTableMetaData((AlterTableStatement) sqlStatement);
        } else if (sqlStatement instanceof DropTableStatement) {
            refreshTableMetaData((DropTableStatement) sqlStatement);
        } else if (sqlStatement instanceof CreateIndexStatement) {
            refreshTableMetaData((CreateIndexStatement) sqlStatement);
        } else if (sqlStatement instanceof DropIndexStatement) {
            refreshTableMetaData((DropIndexStatement) sqlStatement);
        }
    }
    
    private void refreshTableMetaData(final CreateTableStatement createTableStatement) {
        String tableName = createTableStatement.getTables().getSingleTableName();
        getMetaData().getTable().put(tableName, getTableMetaDataInitializer(metaData.getDataSource()).load(tableName, shardingRule));
    }
    
    private void refreshTableMetaData(final AlterTableStatement alterTableStatement) {
        String tableName = alterTableStatement.getTables().getSingleTableName();
        getMetaData().getTable().put(tableName, getTableMetaDataInitializer(metaData.getDataSource()).load(tableName, shardingRule));
    }
    
    private void refreshTableMetaData(final DropTableStatement dropTableStatement) {
        for (String each : dropTableStatement.getTables().getTableNames()) {
            getMetaData().getTable().remove(each);
        }
    }
    
    private void refreshTableMetaData(final CreateIndexStatement createIndexStatement) {
        if (Strings.isNullOrEmpty(createIndexStatement.getIndexName())) {
            return;
        }
        String tableName = createIndexStatement.getTables().getSingleTableName();
        getMetaData().getTable().get(tableName).getLogicIndexes().add(createIndexStatement.getIndexName());
    }
    
    private void refreshTableMetaData(final DropIndexStatement dropIndexStatement) {
        if (Strings.isNullOrEmpty(dropIndexStatement.getIndexName())) {
            return;
        }
        Optional<String> logicTableName = getLogicTableName(dropIndexStatement);
        if (logicTableName.isPresent()) {
            getMetaData().getTable().get(logicTableName.get()).getLogicIndexes().remove(dropIndexStatement.getIndexName());
        }
    }
    
    private Optional<String> getLogicTableName(final DropIndexStatement dropIndexStatement) {
        if (dropIndexStatement.getTables().isEmpty()) {
            return getMetaData().getTable().getLogicTableName(dropIndexStatement.getIndexName());
        }
        return Optional.of(dropIndexStatement.getTables().getSingleTableName());
    }
}

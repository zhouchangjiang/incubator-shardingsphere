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

package org.apache.shardingsphere.core.strategy.encrypt;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.api.config.encryptor.EncryptRuleConfiguration;
import org.apache.shardingsphere.api.config.encryptor.EncryptorRuleConfiguration;
import org.apache.shardingsphere.core.rule.ColumnNode;
import org.apache.shardingsphere.spi.encrypt.ShardingEncryptor;
import org.apache.shardingsphere.spi.encrypt.ShardingQueryAssistedEncryptor;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sharding encryptor engine.
 *
 * @author panjuan
 */
@NoArgsConstructor
public final class ShardingEncryptorEngine {
    
    private final Map<String, ShardingEncryptorStrategy> shardingEncryptorStrategies = new LinkedHashMap<>();
    
    public ShardingEncryptorEngine(final EncryptRuleConfiguration encryptRuleConfiguration) {
        for (Entry<String, EncryptorRuleConfiguration> entry : encryptRuleConfiguration.getEncryptorRuleConfigs().entrySet()) {
            shardingEncryptorStrategies.put(entry.getKey(), new ShardingEncryptorStrategy(entry.getValue()));
        }
    }
    
    /**
     * Get sharding encryptor.
     * 
     * @param logicTableName logic table name
     * @param columnName column name
     * @return optional of sharding encryptor
     */
    public Optional<ShardingEncryptor> getShardingEncryptor(final String logicTableName, final String columnName) {
        for (ShardingEncryptorStrategy each : shardingEncryptorStrategies.values()) {
            Optional<ShardingEncryptor> result = each.getShardingEncryptor(logicTableName, columnName);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.absent();
    }
    
    /**
     * Is has sharding query assisted encryptor or not.
     * 
     * @param logicTableName logic table name
     * @return has sharding query assisted encryptor or not
     */
    public boolean isHasShardingQueryAssistedEncryptor(final String logicTableName) {
        for (ShardingEncryptorStrategy each : shardingEncryptorStrategies.values()) {
            if (each.isHasShardingQueryAssistedEncryptor(logicTableName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get assisted query column.
     * 
     * @param logicTableName logic table name
     * @param columnName column name
     * @return assisted query column
     */
    public Optional<String> getAssistedQueryColumn(final String logicTableName, final String columnName) {
        for (ShardingEncryptorStrategy each : shardingEncryptorStrategies.values()) {
            Optional<String> result = each.getAssistedQueryColumn(logicTableName, columnName);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.absent();
    }
    
    /**
     * Get assisted query columns.
     *
     * @param logicTableName logic table name
     * @return assisted query columns
     */
    public Collection<String> getAssistedQueryColumns(final String logicTableName) {
        Collection<String> assistedQueryColumns = new HashSet<>();
        for (ShardingEncryptorStrategy each : shardingEncryptorStrategies.values()) {
            assistedQueryColumns.addAll(each.getAssistedQueryColumns(logicTableName));
        }
        Collection<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        result.addAll(assistedQueryColumns);
        return result;
    }
    
    /**
     * Get assisted query column count.
     * 
     * @param logicTableName logic table name
     * @return assisted query column count
     */
    public Integer getAssistedQueryColumnCount(final String logicTableName) {
        for (ShardingEncryptorStrategy each : shardingEncryptorStrategies.values()) {
            int result = each.getAssistedQueryColumnCount(logicTableName);
            if (result > 0) {
                return result;
            }
        }
        return 0;
    }
    
    /**
     * Get encrypt table names.
     *
     * @return encrypt table names
     */
    public Collection<String> getEncryptTableNames() {
        Set<String> result = new LinkedHashSet<>();
        for (ShardingEncryptorStrategy each : shardingEncryptorStrategies.values()) {
            result.addAll(each.getEncryptTableNames());
        }
        return result;
    }
    
    /**
     * Get encrypt assisted column values.
     * 
     * @param columnNode column node
     * @param originalColumnValues original column values
     * @return assisted column values
     */
    public List<Comparable<?>> getEncryptAssistedColumnValues(final ColumnNode columnNode, final List<Comparable<?>> originalColumnValues) {
        final Optional<ShardingEncryptor> shardingEncryptor = getShardingEncryptor(columnNode.getTableName(), columnNode.getColumnName());
        Preconditions.checkArgument(shardingEncryptor.isPresent() && shardingEncryptor.get() instanceof ShardingQueryAssistedEncryptor,
                String.format("Can not find ShardingQueryAssistedEncryptor by %s.", columnNode));
        return Lists.transform(originalColumnValues, new Function<Comparable<?>, Comparable<?>>() {
            
            @Override
            public Comparable<?> apply(final Comparable<?> input) {
                return ((ShardingQueryAssistedEncryptor) shardingEncryptor.get()).queryAssistedEncrypt(input.toString());
            }
        });
    }
    
    /**
     * get encrypt column values.
     * 
     * @param columnNode column node
     * @param originalColumnValues original column values
     * @return encrypt column values
     */
    public List<Comparable<?>> getEncryptColumnValues(final ColumnNode columnNode, final List<Comparable<?>> originalColumnValues) {
        final Optional<ShardingEncryptor> shardingEncryptor = getShardingEncryptor(columnNode.getTableName(), columnNode.getColumnName());
        Preconditions.checkArgument(shardingEncryptor.isPresent(), String.format("Can not find ShardingEncryptor by %s.", columnNode));
        return Lists.transform(originalColumnValues, new Function<Comparable<?>, Comparable<?>>() {
            
            @Override
            public Comparable<?> apply(final Comparable<?> input) {
                return String.valueOf(shardingEncryptor.get().encrypt(input.toString()));
            }
        });
    }
}

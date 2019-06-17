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

package org.apache.shardingsphere.shardingjdbc.executor;

import com.google.common.base.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.shardingsphere.core.constant.properties.ShardingProperties;
import org.apache.shardingsphere.core.constant.properties.ShardingPropertiesConstant;
import org.apache.shardingsphere.core.database.DatabaseTypes;
import org.apache.shardingsphere.core.execute.ShardingExecuteEngine;
import org.apache.shardingsphere.core.execute.sql.execute.threadlocal.ExecutorExceptionHandler;
import org.apache.shardingsphere.core.rule.EncryptRule;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.core.rule.TableRule;
import org.apache.shardingsphere.core.strategy.encrypt.ShardingEncryptorEngine;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.ShardingContext;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.connection.ShardingConnection;
import org.apache.shardingsphere.spi.encrypt.ShardingEncryptor;
import org.apache.shardingsphere.transaction.ShardingTransactionManagerEngine;
import org.apache.shardingsphere.transaction.core.TransactionType;
import org.junit.After;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Getter(AccessLevel.PROTECTED)
public abstract class AbstractBaseExecutorTest {
    
    private ShardingExecuteEngine executeEngine;
    
    private ShardingConnection connection;
    
    @Before
    public void setUp() throws SQLException {
        MockitoAnnotations.initMocks(this);
        ExecutorExceptionHandler.setExceptionThrown(false);
        executeEngine = new ShardingExecuteEngine(Runtime.getRuntime().availableProcessors());
        setConnection();
    }
    
    private void setConnection() throws SQLException {
        ShardingContext shardingContext = mock(ShardingContext.class);
        when(shardingContext.getExecuteEngine()).thenReturn(executeEngine);
        when(shardingContext.getShardingProperties()).thenReturn(getShardingProperties());
        when(shardingContext.getDatabaseType()).thenReturn(DatabaseTypes.getActualDatabaseType("H2"));
        ShardingRule shardingRule = getShardingRule();
        when(shardingContext.getShardingRule()).thenReturn(shardingRule);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));
        Map<String, DataSource> dataSourceSourceMap = new LinkedHashMap<>();
        dataSourceSourceMap.put("ds_0", dataSource);
        dataSourceSourceMap.put("ds_1", dataSource);
        ShardingTransactionManagerEngine shardingTransactionManagerEngine = mock(ShardingTransactionManagerEngine.class);
        connection = new ShardingConnection(dataSourceSourceMap, shardingContext, shardingTransactionManagerEngine, TransactionType.LOCAL);
    }
    
    private ShardingRule getShardingRule() {
        ShardingRule shardingRule = mock(ShardingRule.class);
        when(shardingRule.getLogicTableNames(anyString())).thenReturn(Collections.<String>emptyList());
        ShardingEncryptor shardingEncryptor = mock(ShardingEncryptor.class);
        when(shardingEncryptor.decrypt(anyString())).thenReturn("decryptValue");
        ShardingEncryptorEngine shardingEncryptorEngine = mock(ShardingEncryptorEngine.class);
        when(shardingEncryptorEngine.getShardingEncryptor(anyString(), anyString())).thenReturn(Optional.of(shardingEncryptor));
        EncryptRule encryptRule = mock(EncryptRule.class);
        when(encryptRule.getEncryptorEngine()).thenReturn(shardingEncryptorEngine);
        when(shardingRule.getEncryptRule()).thenReturn(encryptRule);
        when(shardingRule.findTableRuleByActualTable("table_x")).thenReturn(Optional.<TableRule>absent());
        return shardingRule;
    }
    
    private ShardingProperties getShardingProperties() {
        Properties props = new Properties();
        props.setProperty(ShardingPropertiesConstant.MAX_CONNECTIONS_SIZE_PER_QUERY.getKey(), ShardingPropertiesConstant.MAX_CONNECTIONS_SIZE_PER_QUERY.getDefaultValue());
        return new ShardingProperties(props);
    }
    
    @After
    public void tearDown() {
        executeEngine.close();
    }
}

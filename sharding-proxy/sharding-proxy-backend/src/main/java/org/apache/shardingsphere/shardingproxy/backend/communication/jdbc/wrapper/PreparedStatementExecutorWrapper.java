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

package org.apache.shardingsphere.shardingproxy.backend.communication.jdbc.wrapper;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.PreparedQueryShardingEngine;
import org.apache.shardingsphere.core.constant.properties.ShardingPropertiesConstant;
import org.apache.shardingsphere.core.optimize.OptimizeEngineFactory;
import org.apache.shardingsphere.core.optimize.result.OptimizeResult;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dml.DMLStatement;
import org.apache.shardingsphere.core.rewrite.SQLRewriteEngine;
import org.apache.shardingsphere.core.rewrite.rewriter.parameter.ParameterRewriter;
import org.apache.shardingsphere.core.rewrite.rewriter.sql.EncryptSQLRewriter;
import org.apache.shardingsphere.core.rewrite.rewriter.sql.SQLRewriter;
import org.apache.shardingsphere.core.route.RouteUnit;
import org.apache.shardingsphere.core.route.SQLRouteResult;
import org.apache.shardingsphere.core.route.SQLUnit;
import org.apache.shardingsphere.core.route.router.masterslave.MasterSlaveRouter;
import org.apache.shardingsphere.shardingproxy.backend.schema.EncryptSchema;
import org.apache.shardingsphere.shardingproxy.backend.schema.LogicSchema;
import org.apache.shardingsphere.shardingproxy.backend.schema.MasterSlaveSchema;
import org.apache.shardingsphere.shardingproxy.backend.schema.ShardingSchema;
import org.apache.shardingsphere.shardingproxy.context.ShardingProxyContext;
import org.apache.shardingsphere.spi.database.DatabaseType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Executor wrapper for prepared statement.
 *
 * @author zhangliang
 * @author panjuan
 */
@RequiredArgsConstructor
public final class PreparedStatementExecutorWrapper implements JDBCExecutorWrapper {
    
    private static final ShardingProxyContext SHARDING_PROXY_CONTEXT = ShardingProxyContext.getInstance();
    
    private final LogicSchema logicSchema;
    
    private final List<Object> parameters;
    
    @Override
    public SQLRouteResult route(final String sql, final DatabaseType databaseType) {
        if (logicSchema instanceof ShardingSchema) {
            return doShardingRoute(sql, databaseType);
        }
        if (logicSchema instanceof MasterSlaveSchema) {
            return doMasterSlaveRoute(sql);
        }
        return doEncryptRoute(sql);
    }
    
    private SQLRouteResult doShardingRoute(final String sql, final DatabaseType databaseType) {
        PreparedQueryShardingEngine shardingEngine = new PreparedQueryShardingEngine(sql, logicSchema.getShardingRule(), 
                ShardingProxyContext.getInstance().getShardingProperties(), logicSchema.getMetaData(), databaseType, logicSchema.getParsingResultCache());
        return shardingEngine.shard(sql, parameters);
    }
    
    private SQLRouteResult doMasterSlaveRoute(final String sql) {
        SQLStatement sqlStatement = ((MasterSlaveSchema) logicSchema).getParseEngine().parse(sql, true);
        SQLRewriteEngine sqlRewriteEngine = new SQLRewriteEngine(sqlStatement);
        sqlRewriteEngine.init(Collections.<ParameterRewriter>emptyList(), Collections.<SQLRewriter>emptyList());
        String rewriteSQL = sqlRewriteEngine.generateSQL().getSql();
        SQLRouteResult result = new SQLRouteResult(sqlStatement);
        for (String each : new MasterSlaveRouter(((MasterSlaveSchema) logicSchema).getMasterSlaveRule(), ((MasterSlaveSchema) logicSchema).getParseEngine(),
                SHARDING_PROXY_CONTEXT.getShardingProperties().<Boolean>getValue(ShardingPropertiesConstant.SQL_SHOW)).route(rewriteSQL, true)) {
            result.getRouteUnits().add(new RouteUnit(each, new SQLUnit(rewriteSQL, parameters)));
        }
        return result;
    }
    
    private SQLRouteResult doEncryptRoute(final String sql) {
        EncryptSchema encryptSchema = (EncryptSchema) logicSchema;
        SQLStatement sqlStatement = encryptSchema.getParseEngine().parse(sql, true);
        SQLRewriteEngine sqlRewriteEngine = new SQLRewriteEngine(encryptSchema.getEncryptRule(), sqlStatement, parameters);
        OptimizeResult optimizeResult = OptimizeEngineFactory.newInstance(encryptSchema.getEncryptRule(), sqlStatement, parameters).optimize();
        Collection<SQLRewriter> sqlRewriters = new LinkedList<>();
        if (sqlStatement instanceof DMLStatement) {
            sqlRewriters.add(new EncryptSQLRewriter(encryptSchema.getEncryptRule().getEncryptorEngine(), (DMLStatement) sqlStatement, optimizeResult));
        }
        sqlRewriteEngine.init(Collections.<ParameterRewriter>emptyList(), sqlRewriters);
        SQLRouteResult result = new SQLRouteResult(sqlStatement);
        result.getRouteUnits().add(new RouteUnit(logicSchema.getDataSources().keySet().iterator().next(), new SQLUnit(sqlRewriteEngine.generateSQL().getSql(), parameters)));
        return result;
    }
    
    @Override
    public Statement createStatement(final Connection connection, final SQLUnit sqlUnit, final boolean isReturnGeneratedKeys) throws SQLException {
        PreparedStatement result = isReturnGeneratedKeys ? connection.prepareStatement(sqlUnit.getSql(), Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(sqlUnit.getSql());
        for (int i = 0; i < sqlUnit.getParameters().size(); i++) {
            result.setObject(i + 1, sqlUnit.getParameters().get(i));
        }
        return result;
    }
    
    @Override
    public boolean executeSQL(final Statement statement, final String sql, final boolean isReturnGeneratedKeys) throws SQLException {
        return ((PreparedStatement) statement).execute();
    }
}

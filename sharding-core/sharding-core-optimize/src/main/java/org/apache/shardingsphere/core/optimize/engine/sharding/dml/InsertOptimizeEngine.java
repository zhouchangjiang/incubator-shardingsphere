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

package org.apache.shardingsphere.core.optimize.engine.sharding.dml;

import com.google.common.base.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.optimize.GeneratedKey;
import org.apache.shardingsphere.core.optimize.condition.ShardingCondition;
import org.apache.shardingsphere.core.optimize.condition.ShardingConditions;
import org.apache.shardingsphere.core.optimize.engine.OptimizeEngine;
import org.apache.shardingsphere.core.optimize.result.OptimizeResult;
import org.apache.shardingsphere.core.optimize.result.insert.InsertOptimizeResult;
import org.apache.shardingsphere.core.optimize.result.insert.InsertOptimizeResultUnit;
import org.apache.shardingsphere.core.parse.sql.context.condition.AndCondition;
import org.apache.shardingsphere.core.parse.sql.context.condition.Condition;
import org.apache.shardingsphere.core.parse.sql.context.insertvalue.InsertValue;
import org.apache.shardingsphere.core.parse.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.core.parse.sql.statement.dml.InsertStatement;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.core.strategy.route.value.ListRouteValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Insert optimize engine for sharding.
 *
 * @author zhangliang
 * @author maxiaoguang
 * @author panjuan
 */
@RequiredArgsConstructor
public final class InsertOptimizeEngine implements OptimizeEngine {
    
    private final ShardingRule shardingRule;
    
    private final InsertStatement insertStatement;
    
    private final List<Object> parameters;
    
    @Override
    public OptimizeResult optimize() {
        List<AndCondition> andConditions = insertStatement.getShardingConditions().getOrConditions();
        Optional<GeneratedKey> generatedKey = GeneratedKey.getGenerateKey(shardingRule, parameters, insertStatement);
        Iterator<Comparable<?>> generatedKeys = generatedKey.isPresent() ? createGeneratedKeys(generatedKey.get()) : null;
        List<ShardingCondition> shardingConditions = new ArrayList<>(andConditions.size());
        InsertOptimizeResult insertOptimizeResult = new InsertOptimizeResult(insertStatement.getColumnNames());
        int parametersCount = 0;
        for (int i = 0; i < andConditions.size(); i++) {
            InsertValue insertValue = insertStatement.getValues().get(i);
            ExpressionSegment[] currentColumnValues = createCurrentColumnValues(insertValue);
            Object[] currentParameters = createCurrentParameters(parametersCount, insertValue);
            parametersCount = parametersCount + insertValue.getParametersCount();
            ShardingCondition shardingCondition = createShardingCondition(andConditions.get(i));
            insertOptimizeResult.addUnit(currentColumnValues, currentParameters, insertValue.getParametersCount());
            if (isNeededToAppendGeneratedKey()) {
                Comparable<?> currentGeneratedKey = generatedKeys.next();
                fillWithGeneratedKeyName(insertOptimizeResult);
                fillInsertOptimizeResultUnit(insertOptimizeResult.getUnits().get(i), currentGeneratedKey);
                fillShardingCondition(shardingCondition, currentGeneratedKey);
            }
            if (isNeededToAppendQueryAssistedColumn()) {
                fillWithQueryAssistedColumn(insertOptimizeResult, i);
            }
            shardingConditions.add(shardingCondition);
        }
        OptimizeResult result = new OptimizeResult(new ShardingConditions(shardingConditions), insertOptimizeResult);
        if (generatedKey.isPresent()) {
            result.setGeneratedKey(generatedKey.get());
        }
        return result;
    }
    
    private Iterator<Comparable<?>> createGeneratedKeys(final GeneratedKey generatedKey) {
        return isNeededToAppendGeneratedKey() ? generatedKey.getGeneratedKeys().iterator() : null;
    }
    
    private ExpressionSegment[] createCurrentColumnValues(final InsertValue insertValue) {
        ExpressionSegment[] result = new ExpressionSegment[insertValue.getAssignments().size() + getIncrement()];
        insertValue.getAssignments().toArray(result);
        return result;
    }
    
    private Object[] createCurrentParameters(final int beginIndex, final InsertValue insertValue) {
        if (0 == insertValue.getParametersCount()) {
            return new Object[0];
        }
        Object[] result = new Object[insertValue.getParametersCount() + getIncrement()];
        parameters.subList(beginIndex, beginIndex + insertValue.getParametersCount()).toArray(result);
        return result;
    }
    
    private int getIncrement() {
        int result = 0;
        if (isNeededToAppendGeneratedKey()) {
            result += 1;
        }
        if (isNeededToAppendQueryAssistedColumn()) {
            result += shardingRule.getEncryptRule().getEncryptorEngine().getAssistedQueryColumnCount(insertStatement.getTables().getSingleTableName());
        }
        return result;
    }
    
    private boolean isNeededToAppendGeneratedKey() {
        String tableName = insertStatement.getTables().getSingleTableName();
        Optional<String> generateKeyColumn = shardingRule.findGenerateKeyColumnName(tableName);
        int valueSize = insertStatement.getValues().isEmpty() ? 0 : insertStatement.getValues().get(0).getAssignments().size();
        return insertStatement.getColumnNames().size() != valueSize || generateKeyColumn.isPresent() && !insertStatement.getColumnNames().contains(generateKeyColumn.get());
    }
    
    private ShardingCondition createShardingCondition(final AndCondition andCondition) {
        ShardingCondition result = new ShardingCondition();
        result.getShardingValues().addAll(getShardingValues(andCondition));
        return result;
    }
    
    private Collection<ListRouteValue> getShardingValues(final AndCondition andCondition) {
        Collection<ListRouteValue> result = new LinkedList<>();
        for (Condition each : andCondition.getConditions()) {
            result.add(new ListRouteValue<>(each.getColumn().getName(), each.getColumn().getTableName(), each.getConditionValues(parameters)));
        }
        return result;
    }
    
    private void fillWithGeneratedKeyName(final InsertOptimizeResult insertOptimizeResult) {
        String generateKeyColumnName = shardingRule.findGenerateKeyColumnName(insertStatement.getTables().getSingleTableName()).get();
        insertOptimizeResult.getColumnNames().add(generateKeyColumnName);
    }
    
    private void fillShardingCondition(final ShardingCondition shardingCondition, final Comparable<?> currentGeneratedKey) {
        String tableName = insertStatement.getTables().getSingleTableName();
        String generateKeyColumnName = shardingRule.findGenerateKeyColumnName(tableName).get();
        if (shardingRule.isShardingColumn(generateKeyColumnName, tableName)) {
            shardingCondition.getShardingValues().add(new ListRouteValue<>(generateKeyColumnName, tableName, Collections.<Comparable<?>>singletonList(currentGeneratedKey)));
        }
    }
    
    private boolean isNeededToAppendQueryAssistedColumn() {
        return shardingRule.getEncryptRule().getEncryptorEngine().isHasShardingQueryAssistedEncryptor(insertStatement.getTables().getSingleTableName());
    }
    
    private void fillWithQueryAssistedColumn(final InsertOptimizeResult insertOptimizeResult, final int insertOptimizeResultIndex) {
        Collection<String> assistedColumnNames = new LinkedList<>();
        for (String each : insertOptimizeResult.getColumnNames()) {
            InsertOptimizeResultUnit unit = insertOptimizeResult.getUnits().get(insertOptimizeResultIndex);
            Optional<String> assistedColumnName = shardingRule.getEncryptRule().getEncryptorEngine().getAssistedQueryColumn(insertStatement.getTables().getSingleTableName(), each);
            if (assistedColumnName.isPresent()) {
                assistedColumnNames.add(assistedColumnName.get());
                fillInsertOptimizeResultUnit(unit, (Comparable<?>) unit.getColumnValue(each));
            }
        }
        if (!assistedColumnNames.isEmpty()) {
            insertOptimizeResult.getColumnNames().addAll(assistedColumnNames);
        }
    }
    
    private void fillInsertOptimizeResultUnit(final InsertOptimizeResultUnit unit, final Comparable<?> columnValue) {
        if (!parameters.isEmpty()) {
            // TODO fix start index and stop index
            unit.addColumnValue(new ParameterMarkerExpressionSegment(0, 0, parameters.size() - 1));
            unit.addColumnParameter(columnValue);
        } else {
            // TODO fix start index and stop index
            unit.addColumnValue(new LiteralExpressionSegment(0, 0, columnValue));
        }
    }
}

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

package org.apache.shardingsphere.core.optimize.engine.sharding.dql;

import com.google.common.base.Optional;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.constant.ShardingOperator;
import org.apache.shardingsphere.core.exception.ShardingException;
import org.apache.shardingsphere.core.optimize.condition.ShardingCondition;
import org.apache.shardingsphere.core.optimize.condition.ShardingConditions;
import org.apache.shardingsphere.core.optimize.engine.OptimizeEngine;
import org.apache.shardingsphere.core.optimize.pagination.Pagination;
import org.apache.shardingsphere.core.optimize.result.OptimizeResult;
import org.apache.shardingsphere.core.parse.sql.context.condition.AndCondition;
import org.apache.shardingsphere.core.parse.sql.context.condition.Column;
import org.apache.shardingsphere.core.parse.sql.context.condition.Condition;
import org.apache.shardingsphere.core.parse.sql.context.condition.Conditions;
import org.apache.shardingsphere.core.parse.sql.segment.dml.pagination.PaginationValueSegment;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.strategy.route.value.BetweenRouteValue;
import org.apache.shardingsphere.core.strategy.route.value.ListRouteValue;
import org.apache.shardingsphere.core.strategy.route.value.RouteValue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Query optimize engine.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class QueryOptimizeEngine implements OptimizeEngine {
    
    private final SQLStatement sqlStatement;
    
    private final List<Object> parameters;
    
    private final Conditions conditions;
    
    @Override
    public OptimizeResult optimize() {
        List<ShardingCondition> shardingConditions = new ArrayList<>(conditions.getOrConditions().size());
        for (AndCondition each : conditions.getOrConditions()) {
            shardingConditions.add(optimize(each.getConditionsMap()));
        }
        OptimizeResult result = new OptimizeResult(new ShardingConditions(shardingConditions));
        result.setPagination(getPagination().orNull());
        return result;
    }
    
    private ShardingCondition optimize(final Map<Column, List<Condition>> conditionsMap) {
        ShardingCondition result = new ShardingCondition();
        for (Entry<Column, List<Condition>> entry : conditionsMap.entrySet()) {
            try {
                RouteValue shardingValue = optimize(entry.getKey(), entry.getValue());
                if (shardingValue instanceof AlwaysFalseShardingValue) {
                    return new AlwaysFalseShardingCondition();
                }
                result.getShardingValues().add(shardingValue);
            } catch (final ClassCastException ex) {
                throw new ShardingException("Found different types for sharding value `%s`.", entry.getKey());
            }
        }
        return result;
    }
    
    private RouteValue optimize(final Column column, final List<Condition> conditions) {
        List<Comparable<?>> listValue = null;
        Range<Comparable<?>> rangeValue = null;
        for (Condition each : conditions) {
            List<Comparable<?>> conditionValues = each.getConditionValues(parameters);
            if (ShardingOperator.EQUAL == each.getOperator() || ShardingOperator.IN == each.getOperator()) {
                listValue = optimize(conditionValues, listValue);
                if (listValue.isEmpty()) {
                    return new AlwaysFalseShardingValue();
                }
            }
            if (ShardingOperator.BETWEEN == each.getOperator()) {
                try {
                    rangeValue = optimize(Range.range(conditionValues.get(0), BoundType.CLOSED, conditionValues.get(1), BoundType.CLOSED), rangeValue);
                } catch (final IllegalArgumentException ex) {
                    return new AlwaysFalseShardingValue();
                }
            }
        }
        if (null == listValue) {
            return new BetweenRouteValue<>(column.getName(), column.getTableName(), rangeValue);
        }
        if (null == rangeValue) {
            return new ListRouteValue<>(column.getName(), column.getTableName(), listValue);
        }
        listValue = optimize(listValue, rangeValue);
        return listValue.isEmpty() ? new AlwaysFalseShardingValue() : new ListRouteValue<>(column.getName(), column.getTableName(), listValue);
    }
    
    private List<Comparable<?>> optimize(final List<Comparable<?>> value1, final List<Comparable<?>> value2) {
        if (null == value2) {
            return value1;
        }
        value1.retainAll(value2);
        return value1;
    }
    
    private Range<Comparable<?>> optimize(final Range<Comparable<?>> value1, final Range<Comparable<?>> value2) {
        return null == value2 ? value1 : value1.intersection(value2);
    }
    
    private List<Comparable<?>> optimize(final List<Comparable<?>> listValue, final Range<Comparable<?>> rangeValue) {
        List<Comparable<?>> result = new LinkedList<>();
        for (Comparable<?> each : listValue) {
            if (rangeValue.contains(each)) {
                result.add(each);
            }
        }
        return result;
    }
    
    private Optional<Pagination> getPagination() {
        if (sqlStatement instanceof SelectStatement) {
            PaginationValueSegment offsetSegment = ((SelectStatement) sqlStatement).getOffset();
            PaginationValueSegment rowCountSegment = ((SelectStatement) sqlStatement).getRowCount();
            if (null != offsetSegment || null != rowCountSegment) {
                return Optional.of(new Pagination(offsetSegment, rowCountSegment, parameters));
            }
        }
        return Optional.absent();
    }
}

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

package org.apache.shardingsphere.core.optimize.engine.sharding;

import com.google.common.collect.Range;
import org.apache.shardingsphere.core.optimize.condition.ShardingCondition;
import org.apache.shardingsphere.core.optimize.condition.ShardingConditions;
import org.apache.shardingsphere.core.optimize.engine.sharding.dql.QueryOptimizeEngine;
import org.apache.shardingsphere.core.parse.sql.context.condition.AndCondition;
import org.apache.shardingsphere.core.parse.sql.context.condition.Column;
import org.apache.shardingsphere.core.parse.sql.context.condition.Condition;
import org.apache.shardingsphere.core.parse.sql.context.condition.Conditions;
import org.apache.shardingsphere.core.parse.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.strategy.route.value.BetweenRouteValue;
import org.apache.shardingsphere.core.strategy.route.value.ListRouteValue;
import org.apache.shardingsphere.core.strategy.route.value.RouteValue;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class QueryOptimizeEngineTest {
    
    @Test
    public void assertOptimizeAlwaysFalseListConditions() {
        Condition condition1 = new Condition(new Column("column", "tbl"), null, Arrays.<ExpressionSegment>asList(new LiteralExpressionSegment(0, 0, 1), new LiteralExpressionSegment(0, 0, 2)));
        Condition condition2 = new Condition(new Column("column", "tbl"), null, new LiteralExpressionSegment(0, 0, 3));
        AndCondition andCondition = new AndCondition();
        andCondition.getConditions().add(condition1);
        andCondition.getConditions().add(condition2);
        Conditions conditions = new Conditions();
        conditions.getOrConditions().add(andCondition);
        ShardingConditions shardingConditions = new QueryOptimizeEngine(new SelectStatement(), Collections.emptyList(), conditions).optimize().getShardingConditions();
        assertTrue(shardingConditions.isAlwaysFalse());
    }
    
    @Test
    public void assertOptimizeAlwaysFalseRangeConditions() {
        Condition condition1 = new Condition(new Column("column", "tbl"), null, new LiteralExpressionSegment(0, 0, 1), new LiteralExpressionSegment(0, 0, 2));
        Condition condition2 = new Condition(new Column("column", "tbl"), null, new LiteralExpressionSegment(0, 0, 3), new LiteralExpressionSegment(0, 0, 4));
        AndCondition andCondition = new AndCondition();
        andCondition.getConditions().add(condition1);
        andCondition.getConditions().add(condition2);
        Conditions conditions = new Conditions();
        conditions.getOrConditions().add(andCondition);
        ShardingConditions shardingConditions = new QueryOptimizeEngine(new SelectStatement(), Collections.emptyList(), conditions).optimize().getShardingConditions();
        assertTrue(shardingConditions.isAlwaysFalse());
    }
    
    @Test
    public void assertOptimizeAlwaysFalseListConditionsAndRangeConditions() {
        Condition condition1 = new Condition(new Column("column", "tbl"), null, Arrays.<ExpressionSegment>asList(new LiteralExpressionSegment(0, 0, 1), new LiteralExpressionSegment(0, 0, 2)));
        Condition condition2 = new Condition(new Column("column", "tbl"), null, new LiteralExpressionSegment(0, 0, 3), new LiteralExpressionSegment(0, 0, 4));
        AndCondition andCondition = new AndCondition();
        andCondition.getConditions().add(condition1);
        andCondition.getConditions().add(condition2);
        Conditions conditions = new Conditions();
        conditions.getOrConditions().add(andCondition);
        ShardingConditions shardingConditions = new QueryOptimizeEngine(new SelectStatement(), Collections.emptyList(), conditions).optimize().getShardingConditions();
        assertTrue(shardingConditions.isAlwaysFalse());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void assertOptimizeListConditions() {
        Condition condition1 = new Condition(new Column("column", "tbl"), null, Arrays.<ExpressionSegment>asList(new LiteralExpressionSegment(0, 0, 1), new LiteralExpressionSegment(0, 0, 2)));
        Condition condition2 = new Condition(new Column("column", "tbl"), null, new LiteralExpressionSegment(0, 0, 1));
        AndCondition andCondition = new AndCondition();
        andCondition.getConditions().add(condition1);
        andCondition.getConditions().add(condition2);
        Conditions conditions = new Conditions();
        conditions.getOrConditions().add(andCondition);
        ShardingConditions shardingConditions = new QueryOptimizeEngine(new SelectStatement(), Collections.emptyList(), conditions).optimize().getShardingConditions();
        assertFalse(shardingConditions.isAlwaysFalse());
        ShardingCondition shardingCondition = shardingConditions.getShardingConditions().get(0);
        RouteValue shardingValue = shardingCondition.getShardingValues().get(0);
        Collection<Comparable<?>> values = ((ListRouteValue<Comparable<?>>) shardingValue).getValues();
        assertThat(values.size(), is(1));
        assertTrue(values.containsAll(Collections.singleton(1)));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void assertOptimizeRangeConditions() {
        Condition condition1 = new Condition(new Column("column", "tbl"), null, new LiteralExpressionSegment(0, 0, 1), new LiteralExpressionSegment(0, 0, 2));
        Condition condition2 = new Condition(new Column("column", "tbl"), null, new LiteralExpressionSegment(0, 0, 1), new LiteralExpressionSegment(0, 0, 3));
        AndCondition andCondition = new AndCondition();
        andCondition.getConditions().add(condition1);
        andCondition.getConditions().add(condition2);
        Conditions parseCondition = new Conditions();
        parseCondition.getOrConditions().add(andCondition);
        ShardingConditions shardingConditions = new QueryOptimizeEngine(new SelectStatement(), Collections.emptyList(), parseCondition).optimize().getShardingConditions();
        assertFalse(shardingConditions.isAlwaysFalse());
        ShardingCondition shardingCondition = shardingConditions.getShardingConditions().get(0);
        RouteValue shardingValue = shardingCondition.getShardingValues().get(0);
        Range<Comparable<?>> values = ((BetweenRouteValue<Comparable<?>>) shardingValue).getValueRange();
        assertThat(values.lowerEndpoint(), CoreMatchers.<Comparable>is(1));
        assertThat(values.upperEndpoint(), CoreMatchers.<Comparable>is(2));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void assertOptimizeListConditionsAndRangeConditions() {
        Condition condition1 = new Condition(new Column("column", "tbl"), null, Arrays.<ExpressionSegment>asList(new LiteralExpressionSegment(0, 0, 1), new LiteralExpressionSegment(0, 0, 2)));
        Condition condition2 = new Condition(new Column("column", "tbl"), null, new LiteralExpressionSegment(0, 0, 1), new LiteralExpressionSegment(0, 0, 2));
        AndCondition andCondition = new AndCondition();
        andCondition.getConditions().add(condition1);
        andCondition.getConditions().add(condition2);
        Conditions parseCondition = new Conditions();
        parseCondition.getOrConditions().add(andCondition);
        ShardingConditions shardingConditions = new QueryOptimizeEngine(new SelectStatement(), Collections.emptyList(), parseCondition).optimize().getShardingConditions();
        assertFalse(shardingConditions.isAlwaysFalse());
        ShardingCondition shardingCondition = shardingConditions.getShardingConditions().get(0);
        RouteValue shardingValue = shardingCondition.getShardingValues().get(0);
        Collection<Comparable<?>> values = ((ListRouteValue<Comparable<?>>) shardingValue).getValues();
        assertThat(values.size(), is(2));
        assertTrue(values.containsAll(Arrays.asList(1, 2)));
    }
}

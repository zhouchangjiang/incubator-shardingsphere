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

package org.apache.shardingsphere.core.merge.dql.orderby;

import com.google.common.collect.Lists;
import org.apache.shardingsphere.core.constant.OrderDirection;
import org.apache.shardingsphere.core.database.DatabaseTypes;
import org.apache.shardingsphere.core.execute.sql.execute.result.QueryResult;
import org.apache.shardingsphere.core.merge.MergedResult;
import org.apache.shardingsphere.core.merge.dql.DQLMergeEngine;
import org.apache.shardingsphere.core.merge.fixture.TestQueryResult;
import org.apache.shardingsphere.core.optimize.condition.ShardingCondition;
import org.apache.shardingsphere.core.optimize.condition.ShardingConditions;
import org.apache.shardingsphere.core.optimize.result.OptimizeResult;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.route.SQLRouteResult;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class OrderByStreamMergedResultTest {
    
    private DQLMergeEngine mergeEngine;
    
    private List<QueryResult> queryResults;
    
    private SQLRouteResult routeResult;
    
    @Before
    public void setUp() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        queryResults = Lists.<QueryResult>newArrayList(
                new TestQueryResult(resultSet), new TestQueryResult(mock(ResultSet.class)), new TestQueryResult(mock(ResultSet.class)));
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.getOrderByItems().add(new IndexOrderByItemSegment(0, 0, 1, OrderDirection.ASC, OrderDirection.ASC));
        routeResult = new SQLRouteResult(selectStatement);
        routeResult.setOptimizeResult(new OptimizeResult(new ShardingConditions(Collections.<ShardingCondition>emptyList())));
    }
    
    @Test
    public void assertNextForResultSetsAllEmpty() throws SQLException {
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        MergedResult actual = mergeEngine.merge();
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForSomeResultSetsEmpty() throws SQLException {
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        when(queryResults.get(0).next()).thenReturn(true, false);
        when(queryResults.get(0).getValue(1, Object.class)).thenReturn("2");
        when(queryResults.get(2).next()).thenReturn(true, true, false);
        when(queryResults.get(2).getValue(1, Object.class)).thenReturn("1", "1", "3", "3");
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("1"));
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("2"));
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("3"));
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForMix() throws SQLException {
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        when(queryResults.get(0).next()).thenReturn(true, false);
        when(queryResults.get(0).getValue(1, Object.class)).thenReturn("2");
        when(queryResults.get(1).next()).thenReturn(true, true, true, false);
        when(queryResults.get(1).getValue(1, Object.class)).thenReturn("2", "2", "3", "3", "4", "4");
        when(queryResults.get(2).next()).thenReturn(true, true, false);
        when(queryResults.get(2).getValue(1, Object.class)).thenReturn("1", "1", "3", "3");
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("1"));
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("2"));
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("2"));
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("3"));
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("3"));
        assertTrue(actual.next());
        assertThat(actual.getValue(1, Object.class).toString(), is("4"));
        assertFalse(actual.next());
    }
}

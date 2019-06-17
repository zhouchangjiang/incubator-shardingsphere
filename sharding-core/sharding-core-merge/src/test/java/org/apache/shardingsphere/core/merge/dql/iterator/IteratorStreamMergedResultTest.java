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

package org.apache.shardingsphere.core.merge.dql.iterator;

import com.google.common.collect.Lists;
import org.apache.shardingsphere.core.database.DatabaseTypes;
import org.apache.shardingsphere.core.execute.sql.execute.result.QueryResult;
import org.apache.shardingsphere.core.merge.MergedResult;
import org.apache.shardingsphere.core.merge.dql.DQLMergeEngine;
import org.apache.shardingsphere.core.merge.fixture.TestQueryResult;
import org.apache.shardingsphere.core.optimize.condition.ShardingCondition;
import org.apache.shardingsphere.core.optimize.condition.ShardingConditions;
import org.apache.shardingsphere.core.optimize.result.OptimizeResult;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.route.SQLRouteResult;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class IteratorStreamMergedResultTest {
    
    private DQLMergeEngine mergeEngine;
    
    private List<QueryResult> queryResults;
    
    private SQLRouteResult routeResult;
    
    @Before
    public void setUp() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData resultSetMetaData = mock(ResultSetMetaData.class);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        queryResults = Lists.<QueryResult>newArrayList(new TestQueryResult(resultSet), new TestQueryResult(mock(ResultSet.class)), new TestQueryResult(mock(ResultSet.class)));
        routeResult = new SQLRouteResult(new SelectStatement());
        routeResult.setOptimizeResult(new OptimizeResult(new ShardingConditions(Collections.<ShardingCondition>emptyList())));
    }
    
    @Test
    public void assertNextForResultSetsAllEmpty() throws SQLException {
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        MergedResult actual = mergeEngine.merge();
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForResultSetsAllNotEmpty() throws SQLException {
        for (QueryResult each : queryResults) {
            when(each.next()).thenReturn(true, false);
        }
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertTrue(actual.next());
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForFirstResultSetsNotEmptyOnly() throws SQLException {
        when(queryResults.get(0).next()).thenReturn(true, false);
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForMiddleResultSetsNotEmpty() throws SQLException {
        when(queryResults.get(1).next()).thenReturn(true, false);
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForLastResultSetsNotEmptyOnly() throws SQLException {
        when(queryResults.get(2).next()).thenReturn(true, false);
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
    
    @Test
    public void assertNextForMix() throws SQLException {
        queryResults.add(new TestQueryResult(mock(ResultSet.class)));
        queryResults.add(new TestQueryResult(mock(ResultSet.class)));
        queryResults.add(new TestQueryResult(mock(ResultSet.class)));
        when(queryResults.get(1).next()).thenReturn(true, false);
        when(queryResults.get(3).next()).thenReturn(true, false);
        when(queryResults.get(5).next()).thenReturn(true, false);
        mergeEngine = new DQLMergeEngine(DatabaseTypes.getActualDatabaseType("MySQL"), routeResult, queryResults);
        MergedResult actual = mergeEngine.merge();
        assertTrue(actual.next());
        assertTrue(actual.next());
        assertTrue(actual.next());
        assertFalse(actual.next());
    }
}

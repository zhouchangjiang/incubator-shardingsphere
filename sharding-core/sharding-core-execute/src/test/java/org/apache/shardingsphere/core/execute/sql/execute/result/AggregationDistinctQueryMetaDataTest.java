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

package org.apache.shardingsphere.core.execute.sql.execute.result;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.shardingsphere.core.constant.AggregationType;
import org.apache.shardingsphere.core.exception.ShardingException;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.AggregationDistinctSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.AggregationSelectItem;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AggregationDistinctQueryMetaDataTest {

    private AggregationDistinctQueryMetaData distinctQueryMetaData; 
    
    @Before
    public void setUp() {
        Collection<AggregationDistinctSelectItem> aggregationDistinctSelectItems = new LinkedList<>();
        AggregationDistinctSelectItem distinctCountSelectItem = new AggregationDistinctSelectItem(AggregationType.COUNT, "(DISTINCT order_id)", Optional.of("c"), "order_id");
        AggregationDistinctSelectItem distinctAvgSelectItem = new AggregationDistinctSelectItem(AggregationType.AVG, "(DISTINCT order_id)", Optional.of("a"), "order_id");
        distinctAvgSelectItem.getDerivedAggregationSelectItems().add(new AggregationSelectItem(AggregationType.COUNT, "(DISTINCT order_id)", Optional.of("AVG_DERIVED_COUNT_0")));
        distinctAvgSelectItem.getDerivedAggregationSelectItems().add(new AggregationSelectItem(AggregationType.SUM, "(DISTINCT order_id)", Optional.of("AVG_DERIVED_SUM_0")));
        aggregationDistinctSelectItems.add(distinctCountSelectItem);
        aggregationDistinctSelectItems.add(distinctAvgSelectItem);
        Multimap<String, Integer> columnLabelAndIndexMap = HashMultimap.create();
        columnLabelAndIndexMap.put("c", 1);
        columnLabelAndIndexMap.put("a", 2);
        columnLabelAndIndexMap.put("AVG_DERIVED_COUNT_0", 3);
        columnLabelAndIndexMap.put("AVG_DERIVED_SUM_0", 4);
        distinctQueryMetaData = new AggregationDistinctQueryMetaData(aggregationDistinctSelectItems, columnLabelAndIndexMap);
    }
    
    @Test
    public void assertIsAggregationDistinctColumnIndex() {
        assertTrue(distinctQueryMetaData.isAggregationDistinctColumnIndex(1));
    }
    
    @Test
    public void assertIsAggregationDistinctColumnLabel() {
        assertTrue(distinctQueryMetaData.isAggregationDistinctColumnLabel("c"));
    }
    
    @Test
    public void assertGetAggregationType() {
        AggregationType actual = distinctQueryMetaData.getAggregationType(2);
        assertThat(actual, is(AggregationType.AVG));
    }
    
    @Test
    public void assertIsDerivedCountColumnIndex() {
        assertTrue(distinctQueryMetaData.isDerivedCountColumnIndex(3));
    }
    
    @Test
    public void assertIsDerivedSumColumnIndex() {
        assertTrue(distinctQueryMetaData.isDerivedSumColumnIndex(4));
    }
    
    @Test
    public void assertGetAggregationDistinctColumnIndexByColumnLabel() {
        int actual = distinctQueryMetaData.getAggregationDistinctColumnIndex("a");
        assertThat(actual, is(2));
    }
    
    @Test(expected = ShardingException.class)
    public void assertGetAggregationDistinctColumnIndexByColumnLabelWithException() {
        int actual = distinctQueryMetaData.getAggregationDistinctColumnIndex("f");
        assertThat(actual, is(2));
    }
    
    @Test
    public void assertGetAggregationDistinctColumnIndexBySumIndex() {
        int actual = distinctQueryMetaData.getAggregationDistinctColumnIndex(4);
        assertThat(actual, is(2));
    }
    
    @Test(expected = ShardingException.class)
    public void assertGetAggregationDistinctColumnIndexBySumIndexWithException() {
        int actual = distinctQueryMetaData.getAggregationDistinctColumnIndex(0);
        assertThat(actual, is(2));
    }
    
    @Test
    public void assertGetAggregationDistinctColumnLabel() { 
        String actual = distinctQueryMetaData.getAggregationDistinctColumnLabel(1);
        assertThat(actual, is("c"));
    }
}

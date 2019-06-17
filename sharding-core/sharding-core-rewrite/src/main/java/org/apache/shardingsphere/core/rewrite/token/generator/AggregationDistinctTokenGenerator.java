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

package org.apache.shardingsphere.core.rewrite.token.generator;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import org.apache.shardingsphere.core.parse.constant.DerivedColumn;
import org.apache.shardingsphere.core.parse.sql.segment.SQLSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.SelectItemsSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.item.AggregationDistinctSelectItemSegment;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.rewrite.token.pojo.AggregationDistinctToken;
import org.apache.shardingsphere.core.rule.ShardingRule;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Aggregation distinct token generator.
 *
 * @author panjuan
 */
public final class AggregationDistinctTokenGenerator implements CollectionSQLTokenGenerator<ShardingRule> {
    
    @Override
    public Collection<AggregationDistinctToken> generateSQLTokens(final SQLStatement sqlStatement, final List<Object> parameters, final ShardingRule shardingRule) {
        Collection<AggregationDistinctToken> result = new LinkedList<>();
        for (SQLSegment each : sqlStatement.getSQLSegments()) {
            Collection<AggregationDistinctSelectItemSegment> distinctSelectItemSegments = getAggregationDistinctSelectItemSegment(each);
            if (!distinctSelectItemSegments.isEmpty()) {
                result.addAll(Collections2.transform(distinctSelectItemSegments, new Function<AggregationDistinctSelectItemSegment, AggregationDistinctToken>() {
                    
                    @Override
                    public AggregationDistinctToken apply(final AggregationDistinctSelectItemSegment input) {
                        return createAggregationDistinctToken(input);
                    }
                }));
            }
        }
        return result;
    }
    
    private Collection<AggregationDistinctSelectItemSegment> getAggregationDistinctSelectItemSegment(final SQLSegment sqlSegment) {
        if (sqlSegment instanceof SelectItemsSegment) {
            return ((SelectItemsSegment) sqlSegment).findSelectItemSegments(AggregationDistinctSelectItemSegment.class);
        }
        return Collections.emptyList();
    }
    
    private AggregationDistinctToken createAggregationDistinctToken(final AggregationDistinctSelectItemSegment segment) {
        Optional<String> derivedAlias = Optional.absent();
        if (DerivedColumn.isDerivedColumnName(segment.getAlias().get())) {
            derivedAlias = Optional.of(segment.getAlias().get());
        }
        return new AggregationDistinctToken(segment.getStartIndex(), segment.getStopIndex(), segment.getDistinctExpression(), derivedAlias);
    }
}

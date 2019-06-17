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

import com.google.common.base.Optional;
import org.apache.shardingsphere.core.parse.sql.segment.dml.SelectItemsSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.item.AggregationDistinctSelectItemSegment;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.rewrite.token.pojo.SelectItemPrefixToken;
import org.apache.shardingsphere.core.rule.ShardingRule;

import java.util.List;

/**
 * Select item prefix token generator.
 *
 * @author panjuan
 */
public final class SelectItemPrefixTokenGenerator implements OptionalSQLTokenGenerator<ShardingRule> {
    
    @Override
    public Optional<SelectItemPrefixToken> generateSQLToken(final SQLStatement sqlStatement, final List<Object> parameters, final ShardingRule shardingRule) {
        if (!(sqlStatement instanceof SelectStatement)) {
            return Optional.absent();
        }
        Optional<SelectItemsSegment> selectItemsSegment = sqlStatement.findSQLSegment(SelectItemsSegment.class);
        if (selectItemsSegment.isPresent() && isContainAggregationDistinctSelectItemSegment(selectItemsSegment.get())) {
            return Optional.of(new SelectItemPrefixToken(selectItemsSegment.get().getStartIndex()));
        }
        return Optional.absent();
    }
    
    private boolean isContainAggregationDistinctSelectItemSegment(final SelectItemsSegment selectItemsSegment) {
        return !selectItemsSegment.findSelectItemSegments(AggregationDistinctSelectItemSegment.class).isEmpty();
    }
}

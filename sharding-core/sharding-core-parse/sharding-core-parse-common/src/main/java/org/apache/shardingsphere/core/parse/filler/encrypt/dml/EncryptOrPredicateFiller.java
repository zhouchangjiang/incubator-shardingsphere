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

package org.apache.shardingsphere.core.parse.filler.encrypt.dml;

import com.google.common.base.Optional;
import lombok.Setter;
import org.apache.shardingsphere.core.metadata.table.ShardingTableMetaData;
import org.apache.shardingsphere.core.parse.aware.EncryptRuleAware;
import org.apache.shardingsphere.core.parse.aware.ShardingTableMetaDataAware;
import org.apache.shardingsphere.core.parse.exception.SQLParsingException;
import org.apache.shardingsphere.core.parse.filler.SQLSegmentFiller;
import org.apache.shardingsphere.core.parse.filler.common.dml.PredicateUtils;
import org.apache.shardingsphere.core.parse.sql.context.condition.AndCondition;
import org.apache.shardingsphere.core.parse.sql.context.condition.Column;
import org.apache.shardingsphere.core.parse.sql.context.condition.Condition;
import org.apache.shardingsphere.core.parse.sql.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.core.parse.sql.segment.dml.predicate.OrPredicateSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.predicate.PredicateSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.predicate.value.PredicateBetweenRightValue;
import org.apache.shardingsphere.core.parse.sql.segment.dml.predicate.value.PredicateCompareRightValue;
import org.apache.shardingsphere.core.parse.sql.segment.dml.predicate.value.PredicateInRightValue;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;
import org.apache.shardingsphere.core.parse.sql.statement.dml.DMLStatement;
import org.apache.shardingsphere.core.rule.EncryptRule;

import java.util.Collection;
import java.util.HashSet;

/**
 * Or predicate filler for encrypt.
 *
 * @author duhongjun
 * @author panjuan
 */
@Setter
public final class EncryptOrPredicateFiller implements SQLSegmentFiller<OrPredicateSegment>, EncryptRuleAware, ShardingTableMetaDataAware {
    
    private EncryptRule encryptRule;
    
    private ShardingTableMetaData shardingTableMetaData;
    
    @Override
    public void fill(final OrPredicateSegment sqlSegment, final SQLStatement sqlStatement) {
        if (!(sqlStatement instanceof DMLStatement)) {
            return;
        }
        Collection<Integer> stopIndexes = new HashSet<>();
        for (AndPredicate each : sqlSegment.getAndPredicates()) {
            for (PredicateSegment predicate : each.getPredicates()) {
                if (stopIndexes.add(predicate.getStopIndex())) {
                    fill(predicate, (DMLStatement) sqlStatement);
                }
            }
        }
    }
    
    private void fill(final PredicateSegment predicateSegment, final DMLStatement dmlStatement) {
        Optional<String> tableName = PredicateUtils.findTableName(predicateSegment, dmlStatement, shardingTableMetaData);
        if (!tableName.isPresent() || !isNeedEncrypt(predicateSegment, tableName.get())) {
            return;
        }
        Column column = new Column(predicateSegment.getColumn().getName(), tableName.get());
        Optional<Condition> condition = createCondition(predicateSegment, column);
        if (condition.isPresent()) {
            AndCondition andCondition;
            if (dmlStatement.getEncryptConditions().getOrConditions().isEmpty()) {
                andCondition = new AndCondition();
                dmlStatement.getEncryptConditions().getOrConditions().add(andCondition);
            } else {
                andCondition = dmlStatement.getEncryptConditions().getOrConditions().get(0);
            }
            andCondition.getConditions().add(condition.get());
        }
    }
    
    private Optional<Condition> createCondition(final PredicateSegment predicateSegment, final Column column) {
        if (predicateSegment.getRightValue() instanceof PredicateBetweenRightValue) {
            throw new SQLParsingException("The SQL clause 'BETWEEN...AND...' is unsupported in encrypt rule.");
        }
        if (predicateSegment.getRightValue() instanceof PredicateCompareRightValue) {
            PredicateCompareRightValue compareRightValue = (PredicateCompareRightValue) predicateSegment.getRightValue();
            return isOperatorSupportedWithEncrypt(compareRightValue.getOperator()) 
                    ? PredicateUtils.createCompareCondition(compareRightValue, column, predicateSegment) : Optional.<Condition>absent();
        }
        if (predicateSegment.getRightValue() instanceof PredicateInRightValue) {
            return PredicateUtils.createInCondition((PredicateInRightValue) predicateSegment.getRightValue(), column, predicateSegment);
        }
        return Optional.absent();
    }
    
    private boolean isNeedEncrypt(final PredicateSegment predicate, final String tableName) {
        return encryptRule.getEncryptorEngine().getShardingEncryptor(tableName, predicate.getColumn().getName()).isPresent();
    }
    
    private boolean isOperatorSupportedWithEncrypt(final String operator) {
        return "=".equals(operator) || "<>".equals(operator) || "!=".equals(operator);
    }
}

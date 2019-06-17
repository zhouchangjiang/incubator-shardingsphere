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

package org.apache.shardingsphere.core.parse.sql.statement;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.shardingsphere.core.parse.sql.context.table.Tables;
import org.apache.shardingsphere.core.parse.sql.segment.SQLSegment;

import java.util.Collection;
import java.util.LinkedList;

/**
 * SQL statement abstract class.
 *
 * @author zhangliang
 * @author panjuan
 */
@Getter
@Setter
@ToString
public abstract class AbstractSQLStatement implements SQLStatement {
    
    private final Collection<SQLSegment> sqlSegments = new LinkedList<>();
    
    private final Tables tables = new Tables();
    
    private String logicSQL;
    
    private int parametersCount;
    
    @Override
    public final Collection<SQLSegment> getSQLSegments() {
        return sqlSegments;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public final <T extends SQLSegment> Optional<T> findSQLSegment(final Class<T> sqlSegmentType) {
        for (SQLSegment each : sqlSegments) {
            if (each.getClass().equals(sqlSegmentType)) {
                return Optional.of((T) each);
            }
        }
        return Optional.absent();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public final <T extends SQLSegment> Collection<T> findSQLSegments(final Class<T> sqlSegmentType) {
        Collection<T> result = new LinkedList<>();
        for (SQLSegment each : sqlSegments) {
            if (each.getClass().equals(sqlSegmentType)) {
                result.add((T) each);
            }
        }
        return result;
    }
}

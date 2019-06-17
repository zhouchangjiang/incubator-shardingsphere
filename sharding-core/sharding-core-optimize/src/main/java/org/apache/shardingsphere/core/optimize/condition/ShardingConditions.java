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

package org.apache.shardingsphere.core.optimize.condition;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.optimize.engine.sharding.dql.AlwaysFalseShardingCondition;

import java.util.List;

/**
 * Sharding conditions.
 *
 * @author zhangliang
 * @author maxiaoguang
 */
@RequiredArgsConstructor
@Getter
public final class ShardingConditions {
    
    private final List<ShardingCondition> shardingConditions;
    
    /**
     * Judge sharding conditions is always false or not.
     *
     * @return sharding conditions is always false or not
     */
    public boolean isAlwaysFalse() {
        if (shardingConditions.isEmpty()) {
            return false;
        }
        for (ShardingCondition each : shardingConditions) {
            if (!(each instanceof AlwaysFalseShardingCondition)) {
                return false;
            }
        }
        return true;
    }
}

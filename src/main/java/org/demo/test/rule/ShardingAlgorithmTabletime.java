package org.demo.test.rule;


import io.shardingsphere.core.api.algorithm.sharding.PreciseShardingValue;
import io.shardingsphere.core.api.algorithm.sharding.standard.PreciseShardingAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * @author yuzc
 * @Description: Precise  Algorithm
 * @date 2019/1/10 10:35
 */
@Slf4j
public class ShardingAlgorithmTabletime implements PreciseShardingAlgorithm<String> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
        String logicTableName = preciseShardingValue.getLogicTableName();
        String dateTime = preciseShardingValue.getValue();
        String tableName = logicTableName  + "_" + dateTime;
        for (Object each : collection) {
            if (each.equals(logicTableName)) {
                log.debug("PRECISE TABLE NAME:{}" , tableName);
                return tableName;
            }
        }
        throw new IllegalArgumentException();
    }

}

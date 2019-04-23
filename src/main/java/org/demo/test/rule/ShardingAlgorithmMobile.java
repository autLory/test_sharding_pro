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
public class ShardingAlgorithmMobile implements PreciseShardingAlgorithm<String> {

    private static final int MOBILE_LEN = 11;

    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<String> preciseShardingValue) {
        String logicTableName = preciseShardingValue.getLogicTableName();
        String mobileNo = preciseShardingValue.getValue();
        if (mobileNo.length() != MOBILE_LEN) {
            log.warn("非法手机号");
            throw new IllegalArgumentException();
        }
        String tableName = logicTableName + "_" + mobileNo.substring(9, 11);
        for (Object each : collection) {
            if (each.equals(logicTableName)) {
                log.debug("PRECISE TABLE NAME:{}", tableName);
                return tableName;
            }
        }
        throw new IllegalArgumentException();
    }

}

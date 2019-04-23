package org.demo.test.config;

import org.demo.test.rule.ShardingAlgorithmMobile;
import io.shardingsphere.core.api.ShardingDataSourceFactory;
import io.shardingsphere.core.api.config.ShardingRuleConfiguration;
import io.shardingsphere.core.api.config.TableRuleConfiguration;
import io.shardingsphere.core.api.config.strategy.StandardShardingStrategyConfiguration;
import io.shardingsphere.jdbc.spring.boot.sharding.SpringBootShardingRuleConfigurationProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author luoli
 * create on 2119/4/21
 */

@AutoConfigureAfter(CommonConfig.class)
@Configuration
@MapperScan(basePackages = "com.zhongfeng.test.persistence.mapper.ds1", sqlSessionTemplateRef = "db1SqlSessionTemplate")
public class DataSource1Config {

    @Autowired
    private SpringBootShardingRuleConfigurationProperties shardingProperties;

    private PathMatchingResourcePatternResolver resolver;

    private Environment environment;

    public DataSource1Config(Environment environment, PathMatchingResourcePatternResolver resolver) {
        this.environment = environment;
        this.resolver = resolver;
    }

    @Bean(name = "ds1DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.ds1")
    public DataSource ds1DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "sharding1DataSource")
    public DataSource shardingDataSource() throws SQLException {
        Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();
        dataSourceMap.put("ds1", ds1DataSource());
        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        shardingRuleConfiguration.getTableRuleConfigs().add(getOrderTableRuleConfiguration());
        DataSource shardingDataSource = ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConfiguration, shardingProperties.getConfigMap(), shardingProperties.getProps());
        return shardingDataSource;
    }

    private TableRuleConfiguration getOrderTableRuleConfiguration() {
        TableRuleConfiguration rule = new TableRuleConfiguration();
        //逻辑表名称
        rule.setLogicTable("t_plat_user_black");
        // 表分片策略
        StandardShardingStrategyConfiguration strategyConfiguration =
                new StandardShardingStrategyConfiguration("mobile_no", new ShardingAlgorithmMobile());
        rule.setTableShardingStrategyConfig(strategyConfiguration);
        return rule;
    }

    @Bean(name = "db1TransactionManager")
    @Primary
    public DataSourceTransactionManager db1TransactionManager() throws Exception {
        return new DataSourceTransactionManager(shardingDataSource());

    }

    @Bean(name = "ds1SqlSessionFactory")
    @Primary
    public SqlSessionFactory ds1SqlSessionFactory() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(shardingDataSource());
        bean.setConfigLocation( new ClassPathResource(environment.getProperty("mybatis.config-location")));
        bean.setMapperLocations(resolveMapperLocations(environment.getProperty("sharding.jdbc.ds1.mapper-locations").split(",")));
        return bean.getObject();

    }

    public Resource[] resolveMapperLocations(String[] locations) {
        return Stream.of(Optional.ofNullable(locations).orElse(new String[1]))
                .flatMap(location -> Stream.of(getResources(location)))
                .toArray(Resource[]::new);
    }

    private Resource[] getResources(String location) {
        try {
            return resolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }

    @Bean(name = "db1SqlSessionTemplate")
    @Primary
    public SqlSessionTemplate db1SqlSessionTemplate() throws Exception{
        return new SqlSessionTemplate(ds1SqlSessionFactory());
    }
}

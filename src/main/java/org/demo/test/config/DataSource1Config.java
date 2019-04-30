package org.demo.test.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.plugin.Interceptor;
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
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.util.ObjectUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * @author luoli
 * create on 2119/4/21
 */

@AutoConfigureAfter(CommonConfig.class)
@Configuration
@MapperScan(basePackages = "org.demo.test.persistence.mapper.ds1", sqlSessionTemplateRef = "db1SqlSessionTemplate")
public class DataSource1Config {

    @Autowired
    private SpringBootShardingRuleConfigurationProperties shardingProperties;

    @Autowired
    private DataSource1Properties dataSource1Properties;

    private PathMatchingResourcePatternResolver resolver;

    private Environment environment;

    private Interceptor[] interceptors;

    public DataSource1Config(Environment environment, PathMatchingResourcePatternResolver resolver, ObjectProvider<Interceptor[]> interceptorsProvider) {
        this.environment = environment;
        this.resolver = resolver;
        this.interceptors = interceptorsProvider.getIfAvailable();
    }

    @Bean(name = "ds1DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.ds1")
    public DataSource ds1DataSource() throws SQLException{
        DataSource ds0DataSource = getDataSource();
        return ds0DataSource;
    }

    public DataSource getDataSource() throws SQLException {
        DruidDataSource result = new DruidDataSource();
        result.setDriverClassName(dataSource1Properties.getDriverClassName());
        result.setUrl(dataSource1Properties.getJdbcURL());
        result.setUsername(dataSource1Properties.getUsername());
        result.setPassword(dataSource1Properties.getPassword());
        result.setInitialSize(dataSource1Properties.getInitialSize());
        result.setMinIdle(dataSource1Properties.getMinIdle());
        result.setMaxActive(dataSource1Properties.getMaxActive());
        result.setMaxWait(dataSource1Properties.getMaxWait());
        result.setTimeBetweenEvictionRunsMillis(dataSource1Properties.getTimeBetweenEvictionRunsMillis());
        result.setValidationQuery(dataSource1Properties.getValidationQuery());
        result.setTestWhileIdle(dataSource1Properties.getTestWhileIdle());
        result.setTestOnBorrow(dataSource1Properties.getTestOnBorrow());
        result.setTestOnReturn(dataSource1Properties.getTestOnReturn());
        result.setPoolPreparedStatements(dataSource1Properties.getPoolPreparedStatements());
        result.setMaxPoolPreparedStatementPerConnectionSize(dataSource1Properties.getMaxPoolPreparedStatementPerConnectionSize());
        result.setFilters(dataSource1Properties.getFilters());
        String[] split = dataSource1Properties.getConnectionProperties().split(";");
        Properties properties = new Properties();
        for (String src : split) {
            String key = src.substring(1, src.indexOf('='));
            String value = src.substring(src.indexOf('=') + 1);
            properties.setProperty(key, value);
        }
        result.setConnectProperties(properties);
        return result;
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
        rule.setLogicTable("table_name");
        // 表分片策略
        StandardShardingStrategyConfiguration strategyConfiguration =
                new StandardShardingStrategyConfiguration("mobile_no", new ShardingAlgorithmMobile());
        rule.setTableShardingStrategyConfig(strategyConfiguration);
        return rule;
    }

    @Bean(name = "db1TransactionManager")
    public DataSourceTransactionManager db1TransactionManager() throws Exception {
        return new DataSourceTransactionManager(shardingDataSource());

    }

    @Bean(name = "ds1SqlSessionFactory")
    public SqlSessionFactory ds1SqlSessionFactory() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(shardingDataSource());
        if (!ObjectUtils.isEmpty(this.interceptors)) {
            bean.setPlugins(this.interceptors);
        }
        bean.setConfigLocation(new ClassPathResource(environment.getProperty("mybatis.config-location")));
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
    public SqlSessionTemplate db1SqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(ds1SqlSessionFactory());
    }
}

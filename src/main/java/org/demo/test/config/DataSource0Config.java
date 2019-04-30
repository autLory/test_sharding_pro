package org.demo.test.config;

import com.alibaba.druid.pool.DruidDataSource;
import io.shardingsphere.core.api.ShardingDataSourceFactory;
import io.shardingsphere.jdbc.spring.boot.sharding.SpringBootShardingRuleConfigurationProperties;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * create on 2019/4/20
 */

@AutoConfigureAfter(CommonConfig.class)
@Configuration
@MapperScan(basePackages = "org.demo.test.persistence.mapper.ds0", sqlSessionTemplateRef = "db0SqlSessionTemplate")
@EnableConfigurationProperties({SpringBootShardingRuleConfigurationProperties.class})
public class DataSource0Config {

    @Autowired
    private SpringBootShardingRuleConfigurationProperties shardingProperties;

    private PathMatchingResourcePatternResolver resolver;

    private Environment environment;

    @Autowired
    private DataSource0Properties dataSource0Properties;

    private Interceptor[] interceptors;

    public DataSource0Config(Environment environment, PathMatchingResourcePatternResolver resolver, ObjectProvider<Interceptor[]> interceptorsProvider) {
        this.environment = environment;
        this.resolver = resolver;
        this.interceptors = interceptorsProvider.getIfAvailable();
    }

    @Bean(name = "ds0DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.ds")
    public DataSource ds0DataSource() throws Exception {
        DataSource ds0DataSource = getDataSource();
        return ds0DataSource;
    }

    public DataSource getDataSource() throws Exception {
        DruidDataSource result = new DruidDataSource();
        result.setDriverClassName(dataSource0Properties.getDriverClassName());
        result.setUrl(dataSource0Properties.getJdbcURL());
        result.setUsername(dataSource0Properties.getUsername());
        result.setPassword(dataSource0Properties.getPassword());
        result.setInitialSize(dataSource0Properties.getInitialSize());
        result.setMinIdle(dataSource0Properties.getMinIdle());
        result.setMaxActive(dataSource0Properties.getMaxActive());
        result.setMaxWait(dataSource0Properties.getMaxWait());
        result.setTimeBetweenEvictionRunsMillis(dataSource0Properties.getTimeBetweenEvictionRunsMillis());
        result.setValidationQuery(dataSource0Properties.getValidationQuery());
        result.setTestWhileIdle(dataSource0Properties.getTestWhileIdle());
        result.setTestOnBorrow(dataSource0Properties.getTestOnBorrow());
        result.setTestOnReturn(dataSource0Properties.getTestOnReturn());
        result.setPoolPreparedStatements(dataSource0Properties.getPoolPreparedStatements());
        result.setMaxPoolPreparedStatementPerConnectionSize(dataSource0Properties.getMaxPoolPreparedStatementPerConnectionSize());
        result.setFilters(dataSource0Properties.getFilters());
        String[] split = dataSource0Properties.getConnectionProperties().split(";");
        Properties properties = new Properties();
        for (String src : split) {
            String key = src.substring(1, src.indexOf('='));
            String value = src.substring(src.indexOf('=') + 1);
            properties.setProperty(key, value);
        }
        result.setConnectProperties(properties);
        result.init();
        return result;
    }

    @Bean(name = "shardingDataSource")
    @Primary
    public DataSource shardingDataSource() throws Exception {
        Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();
        dataSourceMap.put("ds0", ds0DataSource());
        return ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingProperties.getShardingRuleConfiguration(), shardingProperties.getConfigMap(), shardingProperties.getProps());
    }


    @Bean(name = "db0TransactionManager")
    @Primary
    public DataSourceTransactionManager db0TransactionManager() throws Exception {
        return new DataSourceTransactionManager(shardingDataSource());

    }

    @Bean(name = "ds0SqlSessionFactory")
    @Primary
    public SqlSessionFactory ds0SqlSessionFactory() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(shardingDataSource());
        bean.setConfigLocation(new ClassPathResource(environment.getProperty("mybatis.config-location")));
        if (!ObjectUtils.isEmpty(this.interceptors)) {
            bean.setPlugins(this.interceptors);
        }
        bean.setMapperLocations(resolveMapperLocations(environment.getProperty("mybatis.mapper-locations").split(",")));
        return bean.getObject();

    }


    public Resource[] resolveMapperLocations(String[] locations) {
        return Stream.of(Optional.ofNullable(locations).orElse(new String[0]))
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

    @Bean(name = "db0SqlSessionTemplate")
    @Primary
    public SqlSessionTemplate db0SqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(ds0SqlSessionFactory());
    }

}

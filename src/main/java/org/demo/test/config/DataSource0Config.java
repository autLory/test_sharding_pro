package org.demo.test.config;

import io.shardingsphere.core.api.ShardingDataSourceFactory;
import io.shardingsphere.jdbc.spring.boot.sharding.SpringBootShardingRuleConfigurationProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
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

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author luoli
 * create on 2019/4/20
 */

@AutoConfigureAfter(CommonConfig.class)
@Configuration
@MapperScan(basePackages = "com.zhongfeng.test.persistence.mapper.ds0", sqlSessionTemplateRef = "db0SqlSessionTemplate")
@EnableConfigurationProperties({SpringBootShardingRuleConfigurationProperties.class})
public class DataSource0Config {

    @Autowired
    private SpringBootShardingRuleConfigurationProperties shardingProperties;

    private PathMatchingResourcePatternResolver resolver;

    private Environment environment;

    public DataSource0Config(Environment environment, PathMatchingResourcePatternResolver resolver) {
        this.environment = environment;
        this.resolver = resolver;
    }

    @Bean(name = "ds0DataSource")
    @ConfigurationProperties(prefix = "spring.datasource.ds0")
    public DataSource ds0DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "sharding0DataSource")
    @Primary
    public DataSource sharding0DataSource() throws SQLException {
        Map<String, DataSource> dataSourceMap = new LinkedHashMap<>();
        dataSourceMap.put("ds0", ds0DataSource());
        return ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingProperties.getShardingRuleConfiguration(), shardingProperties.getConfigMap(), shardingProperties.getProps());
    }


    @Bean(name = "db0TransactionManager")
    @Primary
    public DataSourceTransactionManager db0TransactionManager() throws Exception {
        return new DataSourceTransactionManager(sharding0DataSource());

    }

    @Bean(name = "ds0SqlSessionFactory")
    @Primary
    public SqlSessionFactory ds0SqlSessionFactory() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(sharding0DataSource());
        bean.setConfigLocation( new ClassPathResource(environment.getProperty("mybatis.config-location")));
        bean.setMapperLocations(resolveMapperLocations(environment.getProperty("sharding.jdbc.ds0.mapper-locations").split(",")));
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

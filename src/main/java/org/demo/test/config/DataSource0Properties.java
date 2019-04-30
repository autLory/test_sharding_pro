package org.demo.test.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

@Configuration
@Data
public class DataSource0Properties implements Serializable {
    private static final long serialVersionUID = -484821303443538121L;

    @Value("${spring.datasource.ds.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.ds.jdbc-url}")
    private String jdbcURL;

    @Value("${spring.datasource.ds.username}")
    private String username;

    @Value("${spring.datasource.ds.password}")
    private String password;

    @Value("${spring.datasource.ds.initial-size:10}")
    private int initialSize;

    @Value("${spring.datasource.ds.min-idle:10}")
    private int minIdle;

    @Value("${spring.datasource.ds.max-active:20}")
    private int maxActive;

    @Value("${spring.datasource.ds.max-wait:60000}")
    private long maxWait;

    @Value("${spring.datasource.ds.timeBetweenEvictionRunsMillis:60000}")
    private long timeBetweenEvictionRunsMillis;

    @Value("${spring.datasource.ds.validationQuery:SELECT now()}")
    private String validationQuery;

    @Value("${spring.datasource.ds.testWhileIdle:true}")
    private Boolean testWhileIdle;

    @Value("${spring.datasource.ds.testOnBorrow:false}")
    private Boolean testOnBorrow;

    @Value("${spring.datasource.ds.testOnReturn:false}")
    private Boolean testOnReturn;

    @Value("${spring.datasource.ds.poolPreparedStatements:true}")
    private Boolean poolPreparedStatements;

    @Value("${spring.datasource.ds.maxPoolPreparedStatementPerConnectionSize:20}")
    private int maxPoolPreparedStatementPerConnectionSize;

    @Value("${spring.datasource.ds.filters:stat,wall}")
    private String filters;

    @Value("${spring.datasource.ds.connectionProperties:druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000}")
    private String connectionProperties;
}

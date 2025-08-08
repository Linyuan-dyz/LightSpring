package com.lightSpring.withoutTX;


import com.lightSpring.JdbcTemplate;
import com.lightspring.Annotations.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;


@ComponentScan
@Configuration
public class JdbcWithoutTxApplication {

    @Bean(destroyMethod = "close")
    public DataSource dataSource(
            // properties:
            @Value("${spring.datasource.url}") String url, //
            @Value("${spring.datasource.username}") String username, //
            @Value("${spring.datasource.password}") String password, //
            @Value("${spring.datasource.driver-class-name:}") String driver, //
            @Value("${spring.datasource.maximum-pool-size:20}") int maximumPoolSize, //
            @Value("${spring.datasource.minimum-pool-size:1}") int minimumPoolSize, //
            @Value("${spring.datasource.connection-timeout:30000}") int connTimeout //
    ) {
        var config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
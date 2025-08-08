package com.lightSpring;

import com.lightSpring.Interface.TransactionalInterface;
import com.lightspring.Annotations.Autowired;
import com.lightspring.Annotations.Bean;
import com.lightspring.Annotations.Configuration;
import com.lightspring.Annotations.Value;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@Configuration
public class JdbcConfiguration {

    @Bean(destroyMethod = "close")
     DataSource dataSource(
            // properties:
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name:}") String driver,
            @Value("${spring.datasource.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${spring.datasource.minimum-pool-size:1}") int minimumPoolSize,
            @Value("${spring.datasource.connection-timeout:30000}") int connTimeout
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
     JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public TransactionalInterface createTransactionInvocationHandler(@Autowired DataSource dataSource) {
        return new TransactionInvocationHandler(dataSource);
    }

    @Bean
    public TransactionPostProcessor createTransactionPostProcessor() {
        return new TransactionPostProcessor();
    }
}
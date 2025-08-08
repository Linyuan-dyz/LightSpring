package com.lightSpring.withTX;


import com.lightSpring.JdbcConfiguration;
import com.lightspring.Annotations.ComponentScan;
import com.lightspring.Annotations.Configuration;
import com.lightspring.Annotations.Import;

@ComponentScan
@Configuration
@Import(JdbcConfiguration.class)
public class JdbcWithTxApplication {

}
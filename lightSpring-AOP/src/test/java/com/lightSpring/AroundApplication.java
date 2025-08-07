package com.lightSpring;


import com.lightSpring.AroundPostProcessor;
import com.lightspring.Annotations.Bean;
import com.lightspring.Annotations.ComponentScan;
import com.lightspring.Annotations.Configuration;

@Configuration
@ComponentScan
public class AroundApplication {

    @Bean
    public AroundPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundPostProcessor();
    }
}
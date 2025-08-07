package com.lightSpring;

import com.lightspring.Annotations.Component;
import com.lightspring.Annotations.Value;

@Component
@Around("AroundInvocationHandler")
public class OriginBean {
    @Value("${customer.name}")
    public String name;

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}

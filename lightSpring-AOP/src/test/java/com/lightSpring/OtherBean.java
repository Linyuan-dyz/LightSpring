package com.lightSpring;


import com.lightspring.Annotations.Autowired;
import com.lightspring.Annotations.Component;
import com.lightspring.Annotations.Order;

@Order(0)
@Component
public class OtherBean {

    public OriginBean origin;

    public OtherBean(@Autowired OriginBean origin) {
        this.origin = origin;
    }
}
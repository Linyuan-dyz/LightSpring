package com.lightspring;

import com.lightspring.Annotations.Autowired;
import com.lightspring.Annotations.Component;

@Component
public class InjectProxyOnConstructorBean {
    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}

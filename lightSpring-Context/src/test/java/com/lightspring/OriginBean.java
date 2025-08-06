package com.lightspring;

import com.lightspring.Annotations.Component;
import com.lightspring.Annotations.Value;

@Component
public class OriginBean {
    @Value("${app.title}")
    public String name;

    @Value("${app.version}")
    public String version;

    public String getName() {
        return name;
    }
}

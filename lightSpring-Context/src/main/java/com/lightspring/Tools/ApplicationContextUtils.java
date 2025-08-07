package com.lightspring.Tools;

import com.lightspring.Context.AnnotationConfigApplicationContext;
import com.lightspring.Context.ApplicationContext;
import com.lightspring.Context.ConfigurableApplicationContext;

public class ApplicationContextUtils {

    private static ConfigurableApplicationContext applicationContext;

    public static ConfigurableApplicationContext getRequiredApplicationContext() {
        if (applicationContext == null) {
            throw new RuntimeException("spring初始化失败");
        }
        return getApplicationContext();
    }

    public static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ConfigurableApplicationContext applicationContext) {
        ApplicationContextUtils.applicationContext = applicationContext;
    }

    public static void closeApplicationContext() {
        setApplicationContext(null);
    }
}

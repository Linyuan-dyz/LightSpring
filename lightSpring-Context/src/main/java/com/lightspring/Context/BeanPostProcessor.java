package com.lightspring.Context;

import java.util.HashMap;
import java.util.Map;

public interface BeanPostProcessor {

    final Map<String, Object> originBeanMap = new HashMap<>();

    default void setOriginBean(Object bean, String beanName) {
        originBeanMap.put(beanName, bean);
    }

    default Object getOriginBean(Object bean, String beanName) {
        Object originBean = originBeanMap.get(beanName);
        if (originBean != null) {
            return originBean;
        }
        return bean;
    }

    private static Object getObject(String beanName) {
        return originBeanMap.get(beanName);
    }

    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
}

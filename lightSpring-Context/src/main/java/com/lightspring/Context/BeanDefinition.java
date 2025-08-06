package com.lightspring.Context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BeanDefinition {
    // 全局唯一的Bean Name:
    String name;

    // Bean的声明类型:
    Class<?> beanClass;

    // Bean的实例:
    Object instance = null;

    // 构造方法/null:
    Constructor<?> constructor;

    // 工厂方法名称/null:
    String factoryMethodName;

    // 工厂方法/null:
    Method factoryMethod;

    // Bean的顺序:
    int order;

    // 是否标识@Primary:
    boolean primary;

    // init/destroy方法名称:
    String initMethodName;
    String destroyMethodName;

    // init/destroy方法:
    Method initMethod;
    Method destroyMethod;

    public BeanDefinition(String name, Class<?> beanClass, Object instance, Constructor<?> constructor, String factoryMethodName, Method factoryMethod, int order, boolean primary, String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.instance = instance;
        this.constructor = constructor;
        this.factoryMethodName = factoryMethodName;
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    public String getName() {
        return name;
    }

    public Method getDestroyMethod() {
        return destroyMethod;
    }

    public Method getInitMethod() {
        return initMethod;
    }

    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    public String getInitMethodName() {
        return initMethodName;
    }

    public boolean isPrimary() {
        return primary;
    }

    public int getOrder() {
        return order;
    }

    public Method getFactoryMethod() {
        return factoryMethod;
    }

    public String getFactoryMethodName() {
        return factoryMethodName;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public Object getInstance() {
        return instance;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDestroyMethod(Method destroyMethod) {
        this.destroyMethod = destroyMethod;
    }

    public void setInitMethod(Method initMethod) {
        this.initMethod = initMethod;
    }

    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setFactoryMethod(Method factoryMethod) {
        this.factoryMethod = factoryMethod;
    }

    public void setFactoryMethodName(String factoryMethodName) {
        this.factoryMethodName = factoryMethodName;
    }

    public void setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }


}

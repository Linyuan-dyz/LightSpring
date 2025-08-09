package com.lightSpring;

class Param {
    // 参数名称:
    String name;
    // 参数类型:
    ParamType paramType;
    // 参数Class类型:
    Class<?> classType;
    // 参数默认值
    String defaultValue;

    public Param(String name, ParamType paramType, Class<?> classType, String defaultValue) {
        this.name = name;
        this.paramType = paramType;
        this.classType = classType;
        this.defaultValue = defaultValue;
    }

    public enum ParamType {
        PATH_VARIABLE,
        REQUEST_PARAM,
        REQUEST_BODY,
        SERVLET_VARIABLE
    }
}

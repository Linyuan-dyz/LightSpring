package com.lightspring.Context;

import org.yaml.snakeyaml.introspector.Property;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public class ResourceResolver {

    private final Map<String, String> propertyMap = new HashMap<>();

    private final Map<Class<?>, Function<String, ?>> converterMap = new HashMap<>();

    private final String resourcePath;

    public ResourceResolver(String resourcePath) {
        this.resourcePath = resourcePath;
        initializeConverterMap();
        initializeProperty();
    }

    public void registerConverter(Class<?> type, Function<String, ?> converter) {
        converterMap.put(type, converter);
    }

    private void initializeConverterMap() {
        // String类型:
        converterMap.put(String.class, s -> s);
        // boolean类型:
        converterMap.put(boolean.class, Boolean::parseBoolean);

        converterMap.put(Boolean.class, Boolean::valueOf);
        // int类型:
        converterMap.put(int.class, Integer::parseInt);
        converterMap.put(Integer.class, Integer::valueOf);
        // 其他基本类型...
        // Date/Time类型:
        converterMap.put(LocalDate.class, LocalDate::parse);
        converterMap.put(LocalTime.class, LocalTime::parse);
        converterMap.put(LocalDateTime.class, LocalDateTime::parse);
        converterMap.put(ZonedDateTime.class, ZonedDateTime::parse);
        converterMap.put(Duration.class, Duration::parse);
        converterMap.put(ZoneId.class, ZoneId::of);
    }

    public void addProperty(PropertyExpr propertyExpr) {
        propertyMap.put(propertyExpr.key, propertyExpr.defaultValue);
    }

    public void addProperties(List<PropertyExpr> propertyExprs) {
        for (PropertyExpr propertyExpr : propertyExprs) {
            addProperty(propertyExpr);
        }
    }

    public <T> T getProperty(String key, Class<T> type) {
        String nestedProperty = getNestedProperty(key);
        if (nestedProperty == null) {
            return null;
        }
        return convertProperty(nestedProperty, type);
    }

    private <T> T convertProperty(String nestedProperty, Class<T> type) {
        Function<String, ?> function = converterMap.get(type);
        if (function == null) {
            throw new RuntimeException("不支持这种类型的转换" + type.getName());
        }
        return (T) function.apply(nestedProperty);
    }

    private String getNestedProperty(String key) {
        String defaultValue = null;
        while (true) {
            PropertyExpr propertyExpr = parsePropertyExpr(key);
            if (propertyExpr != null) {
                // 需要解析的查询
                key = propertyExpr.key;
                defaultValue = propertyExpr.defaultValue;
                if (propertyMap.get(key) != null) {
                    // 可以直接通过key查到值的查询
                    return propertyMap.get(key);
                }
                key = defaultValue;
            } else {
                // 不需要解析的查询
                if (defaultValue == null) {
                    // 第一轮不含defaultVal的查询
                    return propertyMap.get(key);
                }
                if (propertyMap.get(key) == null) {
                    // 默认值查询
                    return defaultValue;
                }
                // 非默认值查询
                return propertyMap.get(key);
            }
        }
    }

    private PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            //需要支持两种形式: 1. ${key} 2. ${key:defaultVal}
            int index = key.indexOf(":");
            if (index == -1) {
                return new PropertyExpr(key.substring("${".length(), key.length() - 1), null);
            } else {
                return new PropertyExpr(key.substring("${".length(), index), key.substring(index + 1, key.length() - 1));
            }
        }
        return null;
    }

    public void fillProperties(Properties propertiess) {
        for (String key : propertiess.stringPropertyNames()) {
            propertyMap.put(key, propertiess.getProperty(key));
        }
    }

    private void initializeProperty() {
        //TODO：利用YamlUtils读取Yml类型文件
        try {
            InputStream inputStream = new FileInputStream(resourcePath);;
            Properties properties = new Properties();
            properties.load(inputStream);
            fillProperties(properties);
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static class PropertyExpr {
        public String key;
        public String defaultValue;

        public PropertyExpr(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
    };
}

package com.lightSpring;

import com.lightspring.Context.BeanPostProcessor;
import com.lightspring.Context.ConfigurableApplicationContext;
import com.lightspring.Tools.ApplicationContextUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class AnnotationPostProcessor <A extends Annotation> implements BeanPostProcessor {

    Class<A> annotationType;

    public AnnotationPostProcessor() {
        this.annotationType = getParameterizedType();
    }

    private Class<A> getParameterizedType() {
        Type type = getClass().getGenericSuperclass();
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type.");
        }
        ParameterizedType pt = (ParameterizedType) type;
        Type[] types = pt.getActualTypeArguments();
        if (types.length != 1) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " has more than 1 parameterized types.");
        }
        Type r = types[0];
        if (!(r instanceof Class<?>)) {
            throw new IllegalArgumentException("Class " + getClass().getName() + " does not have parameterized type of class.");
        }
        return (Class<A>) r;
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();
        A annotation = clazz.getAnnotation(annotationType);
        if (annotation != null) {
            String handlerName = null;
            try {
                handlerName = (String) annotation.annotationType().getMethod("value").invoke(annotation);
                if (handlerName.contains("/")) {
                    handlerName = handlerName.replace("/", ".");
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            if (handlerName == null || handlerName.isEmpty()) {
                return bean;
            }
            if (!handlerName.contains(".")) {
                handlerName = bean.getClass().getPackageName().concat(".").concat(handlerName);
            }
            Object proxy = createProxy(bean, handlerName);
            this.setOriginBean(bean, beanName);
            return proxy;
        }
        return bean;
    }

    protected Object createProxy(Object bean, String handlerName) {
        ConfigurableApplicationContext applicationContext = ApplicationContextUtils.getRequiredApplicationContext();
        InvocationHandler invocationHandler = applicationContext.getBean(handlerName);
        return ProxyResolver.createProxy(bean, invocationHandler);
    }
}

package com.lightspring.Tools;

import com.lightspring.Annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ClassUtils {

    public static String getClassNameByBeanAnnotation(Method method) {
        Bean beanAnnotation = method.getAnnotation(Bean.class);
        String name = beanAnnotation.name();
        if (name.isEmpty()) {
            // TODO:不应该以返回值类型命名
            name = method.getReturnType().getName();
        }
        return name;
    }

    public static boolean getPrimary(Class<?> clazz) {
        Primary primaryAnnotation = clazz.getAnnotation(Primary.class);
        if (primaryAnnotation == null) {
            return false;
        }
        return primaryAnnotation.value();
    }

    public static int getOrder(Class<?> clazz) {
        Order orderAnnotation = clazz.getAnnotation(Order.class);
        if (orderAnnotation != null) {
            return orderAnnotation.value();
        }
        return 0;
    }

    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        A a = target.getAnnotation(annoClass);
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (!annoType.getPackageName().equals("java.lang.annotation")) {
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (a != null) {
                        throw new RuntimeException("Duplicate @" + annoClass.getSimpleName() + " found on class " + target.getSimpleName());
                    }
                    a = found;
                }
            }
        }
        return a;
    }

    public static String getInitMethodNameByBeanAnnotation(Method method) {
        Bean beanAnnotation = method.getAnnotation(Bean.class);
        String initMethodName = beanAnnotation.initMethod();
        if (initMethodName.isEmpty()) {
            return null;
        }
        return initMethodName;
    }

    public static String getDestroyMethodNameByBeanAnnotation(Method method) {
        Bean beanAnnotation = method.getAnnotation(Bean.class);
        String destroyMethodName = beanAnnotation.destroyMethod();
        if (destroyMethodName.isEmpty()) {
            return null;
        }
        return destroyMethodName;
    }

    public static Method getInitMethod(String initMethodName, Class<?> type) throws NoSuchMethodException {
        //TODO:处理多个init方法（即多个PostConstruct注解）的情况
        if (initMethodName == null || initMethodName.isEmpty()) {
            Method[] methods = type.getMethods();
            for (Method method : methods) {
                if (method.getAnnotation(PostConstruct.class) != null) {
                    return method;
                }
            }
            return null;
        }
        return type.getMethod(initMethodName);
    }

    public static Method getDestroyMethod(String destroyMethodName, Class<?> type) throws NoSuchMethodException {
        if (destroyMethodName == null || destroyMethodName.isEmpty()) {
            Method[] methods = type.getMethods();
            for (Method method : methods) {
                if (method.getAnnotation(PreDestroy.class) != null) {
                    return method;
                }
            }
            return null;
        }
        return type.getMethod(destroyMethodName);
    }

    public static boolean isConfiguration(Class<?> type) {
        return type.isAnnotationPresent(Configuration.class);
    }

    public static Constructor<?> getSituatableConstructor(Class<?> clazz) {
        //  TODO:选择构造函数中的参数个数最多的构造函数
        Constructor<?>[] constructors = clazz.getConstructors();
        Constructor<?> situatableConstructor = null;
        int parameterCount = 0;
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() >= parameterCount) {
                situatableConstructor = constructor;
            }
        }
        return situatableConstructor;
    }

    public static Method getAnnotationMethod(Method[] methods, Class<? extends Annotation> annotation) {
        Method targetMethod = null;
        for (Method method : methods) {
            if (method.isAnnotationPresent(annotation)) {
                if (targetMethod == null) {
                    targetMethod = method;
                } else {
                    throw new RuntimeException("同一个类中只能定义一个@PostConstruct修饰的初始化方法");
                }
            }
        }
        return targetMethod;
    }
}

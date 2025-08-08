package com.lightSpring.Annotation;

import com.lightSpring.Enum.TransactionIsolationLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transactional {
    String value() default "com/lightSpring/Interface/TransactionalInterface";
    TransactionIsolationLevel level() default TransactionIsolationLevel.TRANSACTION_NONE;
}


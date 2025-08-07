package com.lightSpring;

import com.lightspring.Annotations.Component;
import com.lightspring.Context.BeanPostProcessor;
import com.lightspring.Context.ConfigurableApplicationContext;
import com.lightspring.Tools.ApplicationContextUtils;

import java.lang.reflect.InvocationHandler;

@Component
public class AroundPostProcessor extends AnnotationPostProcessor<Around> implements BeanPostProcessor {}

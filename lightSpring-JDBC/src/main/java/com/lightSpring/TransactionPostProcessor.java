package com.lightSpring;

import com.lightSpring.Annotation.Transactional;
import com.lightspring.Annotations.Component;

@Component
public class TransactionPostProcessor extends AnnotationPostProcessor<Transactional> {
}

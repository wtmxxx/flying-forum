package com.atcumt.model.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BatchSetConsumerConfig {
    int batchSize() default 20;

    int consumeSize() default -1;

    String messageLog() default "BatchSetConsumer";
}
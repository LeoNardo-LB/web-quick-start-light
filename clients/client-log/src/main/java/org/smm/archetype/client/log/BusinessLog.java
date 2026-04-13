package org.smm.archetype.client.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessLog {
    String value() default "";
    String module() default "";
    OperationType operation() default OperationType.QUERY;
    double samplingRate() default 1.0;
}

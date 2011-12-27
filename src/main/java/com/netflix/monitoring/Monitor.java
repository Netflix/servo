package com.netflix.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Monitor {
    public String name();
    public DataSourceType type() default DataSourceType.INFORMATIONAL;
    public String[] tags() default {};
    public String description() default "";
}

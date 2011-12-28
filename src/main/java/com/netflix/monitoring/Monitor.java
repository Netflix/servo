/*
 * Copyright (c) 2011. Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.netflix.monitoring;

import java.lang.annotation.*;

/**
 * By marking {@link ElementType#FIELD} or {@link ElementType#METHOD} with this
 * annotation, you are telling NOC to monitor the data.
 *
 * @author gkim
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
@Inherited
public @interface Monitor {

    /**
     * This is the name you will use to reference this particular data source
     */
    String dataSourceName();

    /**
     * Description of monitor
     */
    String description() default "";

    /**
     * Specifies the {@link DataSourceType}. Default is {@link DataSourceType#GAUAGE}
     */
    DataSourceType type() default DataSourceType.GAUGE;

    /**
     * Specifies the minimum threshold as String representation.  Default is
     * <tt>""</tt> which means it's undefined.
     * Not applicable to {@link DataSourceType#BOOLEAN}
     */
    String min() default "";

    /**
     * Specifies the maximum threshold as String representation. Default is
     * <tt>""</tt> which means it's undefined.
     * Not applicable to {@link DataSourceType#BOOLEAN}
     */
    String max() default "";

    /**
     * Expected value as String representation.  Only applicable to
     * {@link DataSourceType#BOOLEAN}
     * Default is <tt>""</tt> which means it's undefined.
     */
    String expectedValue() default "";

    /**
     * List of tags to identify the resource.  Format "key=value"
     *
     * @return
     */
    String[] tags() default {};
}

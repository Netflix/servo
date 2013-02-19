/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.annotations;

import com.netflix.servo.tag.Tag;

/**
 * Indicates the type of value that is annotated to determine how it will be
 * measured.
 */
public enum DataSourceType implements Tag {
    /**
     * A gauge is for numeric values that can be sampled without modification.
     * Examples of metrics that should be gauges are things like current
     * temperature, number of open connections, disk usage, etc.
     */
    GAUGE,

    /**
     * A counter is for numeric values that get incremented when some event
     * occurs. Counters will be sampled and converted into a rate of change
     * per second. Counter values should be monotonically increasing, i.e.,
     * the value should not decrease.
     */
    COUNTER,

    /**
     * An informational attribute is for values that might be useful for
     * debugging, but will not be collected as metrics for monitoring purposes.
     * These values are made available in JMX.
     */
    INFORMATIONAL;

    /** Key name used for the data source type tag. */
    public static final String KEY = "type";

    /** {@inheritDoc} */
    public String getKey() {
        return KEY;
    }

    /** {@inheritDoc} */
    public String getValue() {
        return name();
    }

    /** {@inheritDoc} */
    public String tagString() {
        return getKey() + "=" + getValue();
    }
}

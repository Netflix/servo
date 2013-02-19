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
 * Indicates a level for the monitor. This is meant to be similar to log levels to provide a
 * quick way to perform high-level filtering.
 */
public enum DataSourceLevel implements Tag {
    /**
     * Fine granularity monitors that provide a high amount of detail.
     */
    DEBUG,

    /**
     * The default level for monitors.
     */
    INFO,

    /**
     * Most important monitors for an application.
     */
    CRITICAL;

    /** Key name used for the data source level tag. */
    public static final String KEY = "level";

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

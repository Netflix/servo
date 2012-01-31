/*
 * #%L
 * servo-core
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.util;

import com.netflix.servo.MetricConfig;
import com.netflix.servo.TagList;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorId;
import com.netflix.servo.annotations.MonitorTags;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class for a monitored counter value that can be actively updated
 * instead of being polled. After creating an instance of the counter it must
 * be registered with the a {@link com.netflix.servo.MonitorRegistry}.
 */
public final class BasicCounter {

    @MonitorId
    private final String name;

    @MonitorTags
    private final TagList tags;

    @Monitor(name="Count", type=DataSourceType.COUNTER)
    private final AtomicInteger value;

    /**
     * Creates a new instance based on the provide name and empty tag list.
     *
     * @param name  name of the counter
     */
    public BasicCounter(String name) {
        this(new MetricConfig(name));
    }

    /**
     * Creates a new instance based on the provided metric config.
     *
     * @param config  config to associate with the counter
     */
    public BasicCounter(MetricConfig config) {
        this(config.getName(), config.getTags());
    }

    /**
     * Creates a new instance based on the provided metric name and tags.
     *
     * @param name  the name of the metric
     * @param tags  tags to associate with the counter 
     */
    public BasicCounter(String name, TagList tags) {
        this.name = name;
        this.tags = tags;
        this.value = new AtomicInteger(0);
    }

    /** Returns the current value of the counter. */
    public int getValue() {
        return value.get();
    }

    /** Increment the counter by 1. */
    public void increment() {
        value.incrementAndGet();
    }

    /** Increment the counter by {@code delta}. */
    public void increment(int delta) {
        value.getAndAdd(delta);
    }
}

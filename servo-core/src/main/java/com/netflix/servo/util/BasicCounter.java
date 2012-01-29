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

import com.netflix.servo.TagList;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorId;
import com.netflix.servo.annotations.MonitorTags;
import com.netflix.servo.publish.MetricConfig;

import java.util.concurrent.atomic.AtomicInteger;

public final class BasicCounter {

    @MonitorId
    private final String name;

    @MonitorTags
    private final TagList tags;

    @Monitor(name="Count", type=DataSourceType.COUNTER)
    private final AtomicInteger value;

    public BasicCounter(MetricConfig config) {
        this(config.getName(), config.getTags());
    }

    public BasicCounter(String name, TagList tags) {
        this.name = name;
        this.tags = tags;
        this.value = new AtomicInteger(0);
    }

    public int getValue() {
        return value.get();
    }

    public void increment() {
        value.incrementAndGet();
    }

    public void increment(int delta) {
        value.getAndAdd(delta);
    }
}

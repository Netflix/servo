/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
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
package com.netflix.servo;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Represents a metric value at a given point in time.
 */
public final class Metric {
    private final MetricConfig config;
    private final long timestamp;
    private final Number value;

    /**
     * Creates a new instance.
     *
     * @param name       name of the metric
     * @param tags       tags associated with the metric
     * @param timestamp  point in time when the metric value was sampled
     * @param value      value of the metric
     */
    public Metric(String name, TagList tags, long timestamp, Number value) {
        this(new MetricConfig(name, tags), timestamp, value);
    }

    /**
     * Creates a new instance.
     *
     * @param config     config settings associated with the metric
     * @param timestamp  point in time when the metric value was sampled
     * @param value      value of the metric
     */
    public Metric(MetricConfig config, long timestamp, Number value) {
        this.config = Preconditions.checkNotNull(
            config, "config cannot be null");
        this.timestamp = timestamp;
        this.value = Preconditions.checkNotNull(
            value, "value cannot be null (config=%s)", config);
    }

    /** Returns the config settings associated with the metric. */
    public MetricConfig getConfig() {
        return config;
    }

    /** Returns the point in time when the metric was sampled. */
    public long getTimestamp() {
        return timestamp;
    }

    /** Returns the value of the metric. */
    public Number getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Metric)) {
            return false;
        }
        Metric m = (Metric) obj;
        return config.equals(m.getConfig())
            && timestamp == m.getTimestamp()
            && value.equals(m.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, timestamp, value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("config", config)
            .add("timestamp", timestamp)
            .add("value", value)
            .toString();
    }
}

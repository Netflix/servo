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
package com.netflix.servo.monitor;

import com.google.common.base.Objects;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link Gauge} that reports a long value.
 */
public class LongGauge implements Gauge<Long> {
    private final AtomicLong number = new AtomicLong(0L);
    private final MonitorConfig config;

    /**
     * Create a new instance with the specified configuration.
     *
     * @param config   configuration for this gauge
     */
    public LongGauge(MonitorConfig config) {
        this.config = config;
    }

    /**
     * Set the current value.
     */
    public void set(Long n) {
        number.set(n);
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        return number.get();
    }

    /** {@inheritDoc} */
    @Override
    public MonitorConfig getConfig() {
        return config;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof LongGauge)) {
            return false;
        }

        LongGauge that = (LongGauge) o;
        return config.equals(that.config) && number.equals(that.number);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(number, config);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).
                add("number", number).
                add("config", config).
                toString();
    }
}

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
public class LongGauge extends NumberGauge {
    private final AtomicLong number;

    /**
     * Create a new instance with the specified configuration.
     *
     * @param config   configuration for this gauge
     */
    public LongGauge(MonitorConfig config) {
        super(config, new AtomicLong(0L));
        this.number = (AtomicLong) getValue();
    }

    /**
     * Set the current value.
     */
    public void set(Long n) {
        number.set(n);
    }

    /**
     * Returns a reference to the {@link java.util.concurrent.atomic.AtomicLong}.
     */
    public AtomicLong getNumber() {
        return number;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LongGauge that = (LongGauge) o;

        return getConfig().equals(that.getConfig()) && number.get() == that.number.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(number.get(), getConfig());
    }
}

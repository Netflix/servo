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
import com.google.common.util.concurrent.AtomicDouble;

/**
 * A {@link Gauge} that reports a double value.
 */
public class DoubleGauge implements Gauge<Double> {
    private final AtomicDouble number = new AtomicDouble(0.0);
    private final MonitorConfig config;

    /**
     * Create a new instance with the specified configuration.
     *
     * @param config   configuration for this gauge
     */
    public DoubleGauge(MonitorConfig config) {
        this.config = config;
    }

    /**
     * Set the current value.
     */
    public void set(Double n) {
        number.set(n);
    }

    /** {@inheritDoc} */
    @Override
    public Double getValue() {
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

        if (!(o instanceof DoubleGauge)) {
            return false;
        }
        final DoubleGauge that = (DoubleGauge) o;
        // AtomicDouble does not implement a proper .equals so we need to compare the
        // underlying double
        return config.equals(that.config) && number.get() == that.number.get();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        // AtomicDouble does not implement a proper .hashCode() so we need to use the
        // underlying double
        return Objects.hashCode(number.get(), config);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("number", number).add("config", config).toString();
    }
}

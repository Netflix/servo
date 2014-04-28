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
import com.google.common.base.Throwables;

import com.netflix.servo.annotations.DataSourceType;

import java.util.concurrent.Callable;

/**
 * A gauge implementation that invokes a specified callable to get the current value.
 */
public final class BasicGauge<T extends Number> extends AbstractMonitor<T> implements Gauge<T> {
    private final Callable<T> function;

    /**
     * Creates a new instance of the gauge.
     *
     * @param config    configuration for this monitor
     * @param function  a function used to fetch the value on demand
     */
    public BasicGauge(MonitorConfig config, Callable<T> function) {
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        this.function = function;
    }

    /** {@inheritDoc} */
    @Override
    public T getValue() {
        try {
            return function.call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof BasicGauge)) {
            return false;
        }
        BasicGauge m = (BasicGauge) obj;
        return config.equals(m.getConfig()) && function.equals(m.function);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, function);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("function", function)
                .toString();
    }
}

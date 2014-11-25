/*
 * Copyright 2014 Netflix, Inc.
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

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.util.Preconditions;

import java.lang.ref.WeakReference;

/**
 * A {@link Gauge} that returns the value stored in {@link Number}.
 */
public class NumberGauge extends AbstractMonitor<Number> implements Gauge<Number> {
    private final WeakReference<Number> numberRef;

    /**
     * Construct a gauge that will store weak reference to the number. The value returned
     * by the monitor will be the value stored in {@code number} or {@code Double.NaN} in case
     * the referred Number has been garbage collected.
     */
    public NumberGauge(MonitorConfig config, Number number) {
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        Preconditions.checkNotNull(number, "number");
        this.numberRef = new WeakReference<Number>(number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number getValue(int pollerIdx) {
        Number n = numberRef.get();
        return n != null ? n : Double.NaN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof NumberGauge)) {
            return false;
        }

        final NumberGauge that = (NumberGauge) o;
        return config.equals(that.config) && getValue().equals(that.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = getConfig().hashCode();
        result = 31 * result + getValue(0).hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NumberGauge{config=" + config + ", number=" + numberRef.get() + '}';
    }

    /**
     * Returns the {@link Number} hold or null if it has been garbage collected.
     */
    protected Number getBackingNumber() {
        return numberRef.get();
    }
}

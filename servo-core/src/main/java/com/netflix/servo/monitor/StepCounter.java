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

import com.netflix.servo.annotations.DataSourceType;

/**
 * A simple counter implementation backed by a StepLong. The value returned is a rate for the
 * previous interval as defined by the step.
 */
public final class StepCounter extends AbstractMonitor<Number> implements Counter {

    private final double stepSeconds;
    private final StepLong count;

    /**
     * Creates a new instance of the counter.
     */
    public StepCounter(MonitorConfig config, long step) {
        this(config, step, Clock.WALL);
    }

    /**
     * Creates a new instance of the counter.
     */
    StepCounter(MonitorConfig config, long step, Clock clock) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        stepSeconds = step / 1000.0;
        count = new StepLong(step, 0L, clock);
    }

    /** {@inheritDoc} */
    @Override
    public void increment() {
        count.current().incrementAndGet();
    }

    /** {@inheritDoc} */
    @Override
    public void increment(long amount) {
        if (amount > 0L) {
            count.current().addAndGet(amount);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Number getValue() {
        final Datapoint dp = count.poll();
        return dp.isUnknown() ? Double.NaN : dp.getValue() / stepSeconds;
    }

    public long getCount() {
        return count.poll().getValue();
    }

    long getCurrentCount() {
        return count.current().get();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("count", getValue())
                .toString();
    }
}

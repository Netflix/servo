/*
 * #%L
 * servo
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
package com.netflix.servo.monitor;

import com.google.common.base.Function;

import com.netflix.servo.tag.TaggingContext;

import java.util.concurrent.atomic.AtomicLong;

public class ContextualCounter extends AbstractContextualMonitor<Long,Counter> implements Counter {

    public ContextualCounter(
            MonitorConfig config,
            TaggingContext context,
            Function<MonitorConfig,Counter> newMonitor) {
        super(config, context, newMonitor);
    }

    /** {@inheritDoc} */
    @Override
    public void increment() {
        getMonitorForCurrentContext().increment();
    }

    /** {@inheritDoc} */
    @Override
    public void increment(long amount) {
        getMonitorForCurrentContext().increment(amount);
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        return getMonitorForCurrentContext().getValue();
    }
}

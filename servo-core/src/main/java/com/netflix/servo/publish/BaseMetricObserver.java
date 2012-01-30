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
package com.netflix.servo.publish;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorId;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class for metric observers that keeps track of the number of calls
 * and number of failures.
 */
public abstract class BaseMetricObserver implements MetricObserver {
    @MonitorId
    private final String name;

    @Monitor(name="UpdateCount", type= DataSourceType.COUNTER,
             description="Total number of times update has been called.")
    private final AtomicInteger updateCount = new AtomicInteger(0);

    @Monitor(name="UpdateFailureCount", type= DataSourceType.COUNTER,
             description="Number of times update failed with an exception.")
    private final AtomicInteger failedUpdateCount = new AtomicInteger(0);

    /** Creates a new instance with a given name. */
    public BaseMetricObserver(String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    /**
     * Update method that should be defined by sub-classes. This method will
     * get invoked and counts will be maintained in the base observer.
     */
    public abstract void updateImpl(List<Metric> metrics);

    /** {@inheritDoc} */
    public final void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        try {
            updateImpl(metrics);
        } catch (Throwable t) {
            failedUpdateCount.incrementAndGet();
            Throwables.propagate(t);
        } finally {
            updateCount.incrementAndGet();
        }
    }

    /** {@inheritDoc} */
    public String getName(){
        return name;
    }

    /**
     * Can be used by sub-classes to increment the failed count if they handle
     * exception internally.
     */
    protected void incrementFailedCount() {
        failedUpdateCount.incrementAndGet();
    }
}

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
package com.netflix.servo.publish;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorTags;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.StandardTagKeys;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.tag.Tags;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class for metric observers that keeps track of the number of calls
 * and number of failures.
 */
public abstract class BaseMetricObserver implements MetricObserver {
    @MonitorTags
    private final TagList tags;

    private final String name;

    /** Total number of times update has been called. */
    @Monitor(name = "updateCount", type = DataSourceType.COUNTER)
    private final AtomicInteger updateCount = new AtomicInteger(0);

    /** Number of times update failed with an exception. */
    @Monitor(name = "updateFailureCount", type = DataSourceType.COUNTER)
    private final AtomicInteger failedUpdateCount = new AtomicInteger(0);

    /** Creates a new instance with a given name. */
    public BaseMetricObserver(String name) {
        final Tag id = Tags.newTag(StandardTagKeys.MONITOR_ID.getKeyName(), name);
        this.name = Preconditions.checkNotNull(name);
        this.tags = BasicTagList.of(id);
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
            throw Throwables.propagate(t);
        } finally {
            updateCount.incrementAndGet();
        }
    }

    /** {@inheritDoc} */
    public final String getName() {
        return name;
    }

    /**
     * Can be used by sub-classes to increment the failed count if they handle
     * exception internally.
     */
    protected final void incrementFailedCount() {
        failedUpdateCount.incrementAndGet();
    }

    /** Returns the total number of times update has been called. */
    public int getUpdateCount() {
        return updateCount.get();
    }

    /** Returns the number of times update failed with an exception. */
    public int getFailedUpdateCount() {
        return failedUpdateCount.get();
    }
}

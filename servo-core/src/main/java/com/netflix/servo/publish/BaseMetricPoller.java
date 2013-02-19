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
import com.google.common.collect.ImmutableList;
import com.netflix.servo.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base class for simple pollers that do not benefit from filtering in advance.
 * Sub-classes implement {@link #pollImpl} to return a list and all filtering
 * will be taken care of by the provided implementation of {@link #poll}.
 */
public abstract class BaseMetricPoller implements MetricPoller {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Return a list of all current metrics for this poller.
     */
    public abstract List<Metric> pollImpl(boolean reset);

    /** {@inheritDoc} */
    public final List<Metric> poll(MetricFilter filter) {
        return poll(filter, false);
    }

    /** {@inheritDoc} */
    public final List<Metric> poll(MetricFilter filter, boolean reset) {
        Preconditions.checkNotNull(filter, "filter cannot be null");
        List<Metric> metrics = pollImpl(reset);
        ImmutableList.Builder<Metric> builder = ImmutableList.builder();
        for (Metric m : metrics) {
            if (filter.matches(m.getConfig())) {
                builder.add(m);
            }
        }

        List<Metric> retainedMetrics = builder.build();
        logger.debug("received {} metrics, retained {} metrics",
            metrics.size(), retainedMetrics.size());

        return retainedMetrics;
    }
}

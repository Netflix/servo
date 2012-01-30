/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
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

import static com.netflix.servo.annotations.DataSourceType.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public abstract List<Metric> pollImpl();

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter) {
        Preconditions.checkNotNull(filter, "filter cannot be null");
        List<Metric> metrics = pollImpl();
        ImmutableList.Builder<Metric> builder = ImmutableList.builder();
        for (Metric m : metrics) {
            if (filter.matches(m.getName(), m.getTags())) {
                builder.add(m);
            }
        }

        List<Metric> retainedMetrics = builder.build();
        logger.debug("received {} metrics, retained {} metrics",
            metrics.size(), retainedMetrics.size());

        return retainedMetrics;
    }
}

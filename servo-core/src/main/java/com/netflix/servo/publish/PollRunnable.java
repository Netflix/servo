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

import java.util.Collection;
import java.util.List;

/**
 * Runnable that will send updates to a collection of observers.
 */
public class PollRunnable implements Runnable {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(PollRunnable.class);

    private final MetricPoller poller;
    private final MetricFilter filter;
    private final boolean reset;
    private final List<MetricObserver> observers;

    /**
     * Creates a new runnable instance that executes poll with the given filter
     * and sends the metrics to all of the given observers.
     */
    public PollRunnable(
            MetricPoller poller,
            MetricFilter filter,
            Collection<MetricObserver> observers) {
        this(poller, filter, false, observers);
    }

    /**
     * Creates a new runnable instance that executes poll with the given filter
     * and sends the metrics to all of the given observers.
     */
    public PollRunnable(
            MetricPoller poller,
            MetricFilter filter,
            boolean reset,
            Collection<MetricObserver> observers) {
        this.poller = Preconditions.checkNotNull(poller);
        this.filter = Preconditions.checkNotNull(filter);
        this.reset = reset;
        this.observers = ImmutableList.copyOf(observers);
    }

    /**
     * Creates a new runnable instance that executes poll with the given filter
     * and sends the metrics to all of the given observers.
     */
    public PollRunnable(
            MetricPoller poller,
            MetricFilter filter,
            MetricObserver... observers) {
        this(poller, filter, false, ImmutableList.copyOf(observers));
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        try {
            List<Metric> metrics = poller.poll(filter, reset);
            for (MetricObserver o : observers) {
                try {
                    o.update(metrics);
                } catch (Throwable t) {
                    LOGGER.warn("failed to send metrics to " + o.getName(), t);
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("failed to poll metrics", t);
        }
    }
}

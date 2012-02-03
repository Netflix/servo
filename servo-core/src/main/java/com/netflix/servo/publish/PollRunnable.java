/*
 * Copyright (c) 2012. Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.netflix.servo.publish;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.netflix.servo.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Runnable that will send updates to a collection of observers.
 * Immutable.
 */
public class PollRunnable implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PollRunnable.class);

    private final ImmutableCollection<MetricObserver> observers;
    private final MetricPoller poller;
    private final MetricFilter filter;

    /**
     *
     * @param metricPoller
     * @param metricFilter
     * @param metricObservers
     */
    public PollRunnable(MetricPoller metricPoller, MetricFilter metricFilter,
                        Collection<MetricObserver> metricObservers) {
        this.poller = metricPoller;
        this.filter = metricFilter;
        this.observers = ImmutableList.copyOf(metricObservers);
    }

    @Override
    public void run() {
        try {
            List<Metric> metrics = poller.poll(filter);

            for (MetricObserver o : observers) {
                try {
                    o.update(metrics);
                } catch (Throwable t) {
                    log.warn("Problem publising metric to " + o.getName(), t);
                }
            }
        } catch (Throwable t) {
            log.warn("Problem polling metrics", t);
        }
    }
}

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

import com.netflix.servo.Metric;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Callable implementation that invokes the {@link MetricPoller#poll} method.
 */
public final class PollCallable implements Callable<List<Metric>> {

    private final MetricPoller poller;
    private final MetricFilter filter;
    private final boolean reset;

    /**
     * Creates a new instance.
     *
     * @param poller  poller to invoke
     * @param filter  filter to pass into the poller
     */
    public PollCallable(MetricPoller poller, MetricFilter filter) {
        this(poller, filter, false);
    }

    /**
     * Creates a new instance.
     *
     * @param poller  poller to invoke
     * @param filter  filter to pass into the poller
     * @param reset   reset flag to pass into the poller
     */
    public PollCallable(MetricPoller poller, MetricFilter filter, boolean reset) {
        this.poller = poller;
        this.filter = filter;
        this.reset = reset;
    }

    /** {@inheritDoc} */
    public List<Metric> call() {
        return poller.poll(filter, reset);
    }
}

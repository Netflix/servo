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

/**
 * A poller that can be used to fetch current values for a list of metrics on
 * demand.
 */
public interface MetricPoller {
    /**
     * Fetch the current values for a set of metrics that match the provided
     * filter. This method should be cheap, thread-safe, and interruptible so
     * that it can be called frequently to collect metrics at a regular
     * interval.
     *
     * @param filter  retricts the set of metrics
     * @return        list of current metric values
     */
    List<Metric> poll(MetricFilter filter);

    /**
     * Fetch the current values for a set of metrics that match the provided
     * filter. This method should be cheap, thread-safe, and interruptible so
     * that it can be called frequently to collect metrics at a regular
     * interval.
     *
     * @param filter  retricts the set of metrics
     * @param reset   should this poller trigger a reset for
     *                {@link com.netflix.servo.monitor.ResettableMonitor}
     * @return        list of current metric values
     */
    List<Metric> poll(MetricFilter filter, boolean reset);
}

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

import com.netflix.servo.monitor.MonitorConfig;

/**
 * Filter that always returns true or false.
 */
public final class BasicMetricFilter implements MetricFilter {

    /** Filter that matches all metrics. */
    public static final MetricFilter MATCH_ALL = new BasicMetricFilter(true);

    /** Filter that does not match any metrics. */
    public static final MetricFilter MATCH_NONE = new BasicMetricFilter(false);

    private final boolean match;

    /**
     * Creates a new instance with a boolean indicating whether it should
     * always match or always fail.
     *
     * @param match  should this filter match?
     */
    public BasicMetricFilter(boolean match) {
        this.match = match;
    }

    /** {@inheritDoc} */
    public boolean matches(MonitorConfig config) {
        return match;
    }
}

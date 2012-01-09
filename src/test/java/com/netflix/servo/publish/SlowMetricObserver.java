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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlowMetricObserver extends BaseMetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(SlowMetricObserver.class);

    private final MetricObserver wrappedObserver;

    private final long delay;

    public SlowMetricObserver(MetricObserver observer, long delay) {
        super("slow");
        this.wrappedObserver = observer;
        this.delay = delay;
    }

    public void update(List<Metric> metrics) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            LOGGER.warn("sleep interrupted", e);
        }
        wrappedObserver.update(metrics);
    }
}

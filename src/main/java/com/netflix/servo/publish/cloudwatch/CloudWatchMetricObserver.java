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
package com.netflix.servo.publish.cloudwatch;

import com.google.common.base.Preconditions;

import com.netflix.servo.publish.BaseMetricObserver;
import com.netflix.servo.publish.Metric;
import com.netflix.servo.publish.MetricObserver;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes observations to Amazon's CloudWatch.
 */
public class CloudWatchMetricObserver extends BaseMetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CloudWatchMetricObserver.class);

    public CloudWatchMetricObserver(String name) {
        super(name);
    }

    public void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
    }
}

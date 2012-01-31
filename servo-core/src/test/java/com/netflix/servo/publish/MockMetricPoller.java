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

import com.google.common.collect.ImmutableList;

import com.netflix.servo.Metric;

import java.util.List;

public class MockMetricPoller extends BaseMetricPoller {

    private List<Metric> metrics;

    public void setMetrics(List<Metric> metrics) {
        this.metrics = ImmutableList.copyOf(metrics);
    }

    public List<Metric> pollImpl() {
        return metrics;
    }
}

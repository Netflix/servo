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
import com.google.common.collect.Lists;

import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxMetricPoller implements MetricPoller {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(JmxMetricPoller.class);

    private final MBeanServerConnection connection;
    private final ObjectName query;

    public JmxMetricPoller(MBeanServerConnection connection, ObjectName query) {
        this.connection = connection;
        this.query = query;
    }

    public List<Metric> poll(MetricFilter filter) {
        return null;
    }
}

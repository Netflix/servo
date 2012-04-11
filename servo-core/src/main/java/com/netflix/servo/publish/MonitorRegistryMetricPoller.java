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

import com.google.common.collect.Lists;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.Metric;
import com.netflix.servo.MonitorContext;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.annotations.AnnotatedAttribute;
import com.netflix.servo.annotations.AnnotatedObject;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Poller for fetching {@link com.netflix.servo.annotations.Monitor} metrics
 * from a monitor registry.
 */
public final class MonitorRegistryMetricPoller implements MetricPoller {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MonitorRegistryMetricPoller.class);

    private final MonitorRegistry registry;

    /**
     * Creates a new instance using
     * {@link com.netflix.servo.DefaultMonitorRegistry}.
     */
    public MonitorRegistryMetricPoller() {
        this(DefaultMonitorRegistry.getInstance());
    }

    /**
     * Creates a new instance using the specified registry.
     *
     * @param registry  registry to query for annotated objects
     */
    public MonitorRegistryMetricPoller(MonitorRegistry registry) {
        this.registry = registry;
    }

    private void getMetrics(
            List<Metric> metrics,
            MetricFilter filter,
            AnnotatedObject obj)
            throws Exception {
        String classId = obj.getId();
        LOGGER.debug("retrieving metrics from class {} id {}",
            obj.getClassName(), classId);

        List<AnnotatedAttribute> attrs = obj.getAttributes();
        for (AnnotatedAttribute attr : attrs) {
            // Skip informational annotations
            Monitor anno = attr.getAnnotation();
            TagList tags = BasicTagList.concat(attr.getTags(), anno.type());
            if (anno.type() == DataSourceType.INFORMATIONAL) {
                continue;
            }

            // Create config and add metric if filter matches 
            MonitorContext config = new MonitorContext.Builder(anno.name()).withTags(tags).build();
            if (filter.matches(config)) {
                Number num = attr.getNumber();
                if (num != null) {
                    long now = System.currentTimeMillis();
                    metrics.add(new Metric(config, now, num));
                } else {
                    LOGGER.debug("expected number but found {}, metric {}",
                        attr.getValue(), config);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter) {
        List<Metric> metrics = Lists.newArrayList();
        for (AnnotatedObject obj : registry.getRegisteredObjects()) {
            try {
                getMetrics(metrics, filter, obj);
            } catch (Exception e) {
                LOGGER.warn("failed to extract metrics from class {}", e,
                    obj.getClass().getCanonicalName());
            }
        }
        return metrics;
    }
}

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.netflix.servo.BasicTag;
import com.netflix.servo.BasicTagList;
import com.netflix.servo.Metric;
import com.netflix.servo.MetricConfig;
import com.netflix.servo.StandardTagKeys;
import com.netflix.servo.Tag;
import com.netflix.servo.TagList;

import com.netflix.servo.annotations.AnnotationUtils;
import com.netflix.servo.annotations.DataSourceType;

import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic poller for fetching simple data from JMX.
 */
public final class JmxMetricPoller implements MetricPoller {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(JmxMetricPoller.class);

    private static final Tag CLASS_TAG = new BasicTag(
        StandardTagKeys.CLASS_NAME.getKeyName(),
        JmxMetricPoller.class.getCanonicalName());

    private static final String DOMAIN_KEY = "JmxDomain";
    private static final String COMPOSITE_PATH_KEY = "JmxCompositePath";
    private static final String PROP_KEY_PREFIX = "Jmx";

    private final JmxConnector connector;
    private final ObjectName query;
    private final MetricFilter counters;

    /**
     * Creates a new instance that polls mbeans matching the provided object
     * name pattern.
     *
     * @param connector  used to get a connection to an MBeanServer
     * @param query      object name pattern for selecting mbeans
     * @param counters   metrics matching this filter will be treated as
     *                   counters, all others will be gauges
     */
    public JmxMetricPoller(
            JmxConnector connector, ObjectName query, MetricFilter counters) {
        this.connector = connector;
        this.query = query;
        this.counters = counters;
    }

    /**
     * Creates a tag list from an object name.
     */
    private BasicTagList createTagList(ObjectName name) {
        Map<String,String> props =
            (Map<String,String>) name.getKeyPropertyList();
        List<Tag> tags = Lists.newArrayList();
        for (Map.Entry<String,String> e : props.entrySet()) {
            String key = PROP_KEY_PREFIX + "." + e.getKey();
            tags.add(new BasicTag(key, e.getValue()));
        }
        tags.add(new BasicTag(DOMAIN_KEY, name.getDomain()));
        tags.add(CLASS_TAG);
        return new BasicTagList(tags);
    }

    /**
     * Create a new metric object and add it to the list.
     */
    private void addMetric(
            List<Metric> metrics,
            String name,
            BasicTagList tags,
            Object value) {
        long now = System.currentTimeMillis();
        Number num = AnnotationUtils.asNumber(value);
        if (num != null) {
            TagList newTags = counters.matches(new MetricConfig(name, tags))
                ? tags.copy(BasicTagList.copyOf(DataSourceType.COUNTER))
                : tags.copy(BasicTagList.copyOf(DataSourceType.GAUGE));
            Metric m = new Metric(name, newTags, now, num);
            metrics.add(m);
        }
    }

    /**
     * Recursively extracts simple numeric values from composite data objects.
     * The map {@code values} will be populated with a path to the value as
     * the key and the simple object as the value.
     */
    private void extractValues(
            String path, Map<String,Object> values, CompositeData obj) {
        for (String key : obj.getCompositeType().keySet()) {
            String newPath = (path == null) ? key : path + "." + key;
            Object value = obj.get(key);
            if (value instanceof CompositeData) {
                extractValues(newPath, values, (CompositeData) value);
            } else if (value != null) {
                values.put(newPath, value);
            }
        }
    }

    /**
     * Query the mbean connection and add all metrics that satisfy the filter
     * to the list {@code metrics}.
     */
    private void getMetrics(
            MBeanServerConnection con,
            MetricFilter filter,
            List<Metric> metrics,
            ObjectName name)
            throws JMException, IOException {
        // Create tags from the object name
        BasicTagList tags = createTagList(name);
        MBeanInfo info = con.getMBeanInfo(name);
        MBeanAttributeInfo[] attrInfos = info.getAttributes();
        if (attrInfos == null) {
            return;
        }

        // Restrict to attributes that match the filter
        List<String> matchingNames = Lists.newArrayList();
        for (MBeanAttributeInfo attrInfo : attrInfos) {
            String attrName = attrInfo.getName();
            if (filter.matches(new MetricConfig(attrName, tags))) {
                matchingNames.add(attrName);
            }
        }

        // Get values for matching attributes
        int size = matchingNames.size();
        String[] attrNames = matchingNames.toArray(new String[size]);
        AttributeList attrs = con.getAttributes(name, attrNames);
        for (Attribute attr : attrs.asList()) {
            String attrName = attr.getName();
            Object obj = attr.getValue();
            if (obj instanceof CompositeData) {
                Map<String,Object> values = Maps.newHashMap();
                extractValues(null, values, (CompositeData) obj);
                for (Map.Entry<String,Object> e : values.entrySet()) {
                    String key = e.getKey();
                    BasicTagList newTags = tags.copy(COMPOSITE_PATH_KEY, key);
                    if (filter.matches(new MetricConfig(attrName, newTags))) {
                        addMetric(metrics, attrName, newTags, e.getValue());
                    }
                }
            } else {
                addMetric(metrics, attrName, tags, obj);
            }
        }
    }

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter) {
        List<Metric> metrics = Lists.newArrayList();
        try {
            MBeanServerConnection con = connector.getConnection();
            Set<ObjectName> names = con.queryNames(query, null);
            for (ObjectName name : names) {
                try {
                    getMetrics(con, filter, metrics, name);
                } catch (JMException e) {
                    LOGGER.warn("failed to get metrics for: " + name, e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("failed to collect jmx metrics matching: " + query, e);
        }
        return metrics;
    }
}

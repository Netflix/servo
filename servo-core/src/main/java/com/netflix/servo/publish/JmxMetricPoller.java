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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.StandardTagKeys;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.tag.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generic poller for fetching simple data from JMX.
 */
public final class JmxMetricPoller implements MetricPoller {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(JmxMetricPoller.class);

    private static final Tag CLASS_TAG = Tags.newTag(
        StandardTagKeys.CLASS_NAME.getKeyName(),
        JmxMetricPoller.class.getCanonicalName());

    private static final String DOMAIN_KEY = "JmxDomain";
    private static final String COMPOSITE_PATH_KEY = "JmxCompositePath";
    private static final String PROP_KEY_PREFIX = "Jmx";

    private final JmxConnector connector;
    private final List<ObjectName> queries;
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
        this.queries = Lists.newArrayList();
        queries.add(query);
        this.counters = counters;
    }

    /**
     * Creates a new instance that polls mbeans matching the provided object
     * name patterns.
     *
     * @param connector  used to get a connection to an MBeanServer
     * @param queries    object name patterns for selecting mbeans
     * @param counters   metrics matching this filter will be treated as
     *                   counters, all others will be gauges
     */
    public JmxMetricPoller(
            JmxConnector connector, List<ObjectName> queries, MetricFilter counters) {
        this.connector = connector;
        this.queries = queries;
        this.counters = counters;
    }

    /**
     * Creates a tag list from an object name.
     */
    private TagList createTagList(ObjectName name) {
        Map<String, String> props = name.getKeyPropertyList();
        List<Tag> tags = Lists.newArrayList();
        for (Map.Entry<String, String> e : props.entrySet()) {
            String key = PROP_KEY_PREFIX + "." + e.getKey();
            tags.add(Tags.newTag(key, e.getValue()));
        }
        tags.add(Tags.newTag(DOMAIN_KEY, name.getDomain()));
        tags.add(CLASS_TAG);
        return SortedTagList.builder().withTags(tags).build();
    }

    /**
     * Create a new metric object and add it to the list.
     */
    private void addMetric(
            List<Metric> metrics,
            String name,
            TagList tags,
            Object value) {
        long now = System.currentTimeMillis();
        Number num = asNumber(value);
        if (num != null) {
            TagList newTags = counters.matches(MonitorConfig.builder(name).withTags(tags).build())
                ? SortedTagList.builder().withTags(tags).withTag(DataSourceType.COUNTER).build()
                : SortedTagList.builder().withTags(tags).withTag(DataSourceType.GAUGE).build();
            Metric m = new Metric(name, newTags, now, num);
            metrics.add(m);
        }
    }

    /**
     * Recursively extracts simple numeric values from composite data objects.
     * The map {@code values} will be populated with a path to the value as
     * the key and the simple object as the value.
     */
    private void extractValues(String path, Map<String, Object> values, CompositeData obj) {
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
        TagList tags = createTagList(name);
        MBeanInfo info = con.getMBeanInfo(name);
        MBeanAttributeInfo[] attrInfos = info.getAttributes();

        // Restrict to attributes that match the filter
        List<String> matchingNames = Lists.newArrayList();
        for (MBeanAttributeInfo attrInfo : attrInfos) {
            String attrName = attrInfo.getName();
            if (filter.matches(new MonitorConfig.Builder(attrName).withTags(tags).build())) {
                matchingNames.add(attrName);
            }
        }
        List<Attribute> attributeList = safelyLoadAttributes(con, name, matchingNames);

        for (Attribute attr : attributeList) {
            String attrName = attr.getName();
            Object obj = attr.getValue();
            if (obj instanceof CompositeData) {
                Map<String, Object> values = Maps.newHashMap();
                extractValues(null, values, (CompositeData) obj);
                for (Map.Entry<String, Object> e : values.entrySet()) {
                    String key = e.getKey();
                    TagList newTags = SortedTagList.builder()
                        .withTags(tags)
                        .withTag(COMPOSITE_PATH_KEY, key)
                        .build();
                    if (filter.matches(MonitorConfig.builder(attrName).withTags(newTags).build())) {
                        addMetric(metrics, attrName, newTags, e.getValue());
                    }
                }
            } else {
                addMetric(metrics, attrName, tags, obj);
            }
        }
    }

    /**
     * Try to convert an object into a number. Boolean values will return 1 if
     * true and 0 if false. If the value is null or an unknown data type null
     * will be returned.
     */
    private static Number asNumber(Object value) {
        Number num = null;
        if (value == null) {
            num = null;
        } else if (value instanceof Number) {
            num = (Number) value;
        } else if (value instanceof Boolean) {
            num = ((Boolean) value) ? 1 : 0;
        }
        return num;
    }

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter) {
        return poll(filter, false);
    }

    /** {@inheritDoc} */
    public List<Metric> poll(MetricFilter filter, boolean reset) {
        List<Metric> metrics = Lists.newArrayList();
        try {
            MBeanServerConnection con = connector.getConnection();
            for (ObjectName query : queries) {
                Set<ObjectName> names = con.queryNames(query, null);
                for (ObjectName name : names) {
                    try {
                        getMetrics(con, filter, metrics, name);
                    } catch (Exception e) {
                        LOGGER.warn("failed to get metrics for: " + name, e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("failed to collect jmx metrics.", e);
        }
        return metrics;
    }

    /**
     * There are issues loading some JMX attributes on some systems. This protects us from a
     * single bad attribute stopping us reading any metrics (or just a random sampling) out of
     * the system.
     */
    private static List<Attribute> safelyLoadAttributes(
            MBeanServerConnection server, ObjectName objectName, List<String> matchingNames) {
        try {
            // first try batch loading all attributes as this is faster
            return batchLoadAttributes(server, objectName, matchingNames);
        } catch (Exception e) {
            // JBOSS ticket: https://issues.jboss.org/browse/AS7-4404

            LOGGER.info("Error batch loading attributes for {} : {}", objectName, e.getMessage());
            // some containers (jboss I am looking at you) fail the entire getAttributes request
            // if one is broken we can get the working attributes if we ask for them individually
            return individuallyLoadAttributes(server, objectName, matchingNames);
        }
    }

    private static List<Attribute> batchLoadAttributes(
            MBeanServerConnection server, ObjectName objectName,  List<String> matchingNames)
            throws InstanceNotFoundException, ReflectionException, IOException {
        final String[] namesArray = matchingNames.toArray(new String[matchingNames.size()]);
        return server.getAttributes(objectName, namesArray).asList();
    }

    private static List<Attribute> individuallyLoadAttributes(
            MBeanServerConnection server, ObjectName objectName, List<String> matchingNames) {
        List<Attribute> attributes = Lists.newArrayList();
        for (String attrName : matchingNames) {
            try {
                Object value = server.getAttribute(objectName, attrName);
                attributes.add(new Attribute(attrName, value));
            } catch (Exception e) {
                LOGGER.info("Couldn't load attribute {} for {} : {}",
                    new Object[] {attrName, objectName, e.getMessage()}, e);
            }
        }
        return attributes;
    }
}

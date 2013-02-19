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

import com.google.common.base.Strings;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

import java.util.Map;
import java.util.NavigableMap;

/**
 * Filter that checks for a prefix match on a given tag. This can be useful
 * providing rules associated with the canonical class name.
 */
public final class PrefixMetricFilter implements MetricFilter {

    private final String tagKey;
    private final MetricFilter root;
    private final NavigableMap<String, MetricFilter> filters;

    /**
     * Creates a new prefix filter.
     *
     * @param tagKey   the tag to perform matching on, if null the name will be
     *                 checked
     * @param root     filter used if there are no prefix matches
     * @param filters  map of prefix to sub-filter. The filter associated with
     *                 the longest matching prefix will be used.
     */
    public PrefixMetricFilter(
            String tagKey,
            MetricFilter root,
            NavigableMap<String, MetricFilter> filters) {
        this.tagKey = tagKey;
        this.root = root;
        this.filters = filters;
    }

    /** {@inheritDoc} */
    public boolean matches(MonitorConfig config) {
        String name = config.getName();
        TagList tags = config.getTags();
        String value;
        if (tagKey == null) {
            value = name;
        } else {
            Tag t = tags.getTag(tagKey);
            value = (t == null) ? null : t.getValue();
        }

        if (Strings.isNullOrEmpty(value)) {
            return root.matches(config);
        } else {
            String start = value.substring(0, 1);
            NavigableMap<String, MetricFilter> candidates =
                filters.subMap(start, true, value, true).descendingMap();
            if (candidates.isEmpty()) {
                return root.matches(config);
            } else {
                for (Map.Entry<String, MetricFilter> e : candidates.entrySet()) {
                    if (value.startsWith(e.getKey())) {
                        return e.getValue().matches(config);
                    }
                }
                return root.matches(config);
            }
        }
    }
}

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
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

import java.util.regex.Pattern;

/**
 * Filter that checks if a tag value matches a regular expression.
 */
public final class RegexMetricFilter implements MetricFilter {

    private final String tagKey;
    private final Pattern pattern;
    private final boolean matchIfMissingTag;
    private final boolean invert;

    /**
     * Creates a new regex filter.
     *
     * @param tagKey             tag to check against the pattern
     * @param pattern            pattern to check
     * @param matchIfMissingTag  should metrics without the specified tag match?
     * @param invert             should the match be inverted?
     */
    public RegexMetricFilter(
            String tagKey,
            Pattern pattern,
            boolean matchIfMissingTag,
            boolean invert) {
        this.tagKey = tagKey;
        this.pattern = pattern;
        this.matchIfMissingTag = matchIfMissingTag;
        this.invert = invert;
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

        boolean match = matchIfMissingTag;
        if (value != null) {
            match = pattern.matcher(value).matches();
        }
        return match ^ invert;
    }
}

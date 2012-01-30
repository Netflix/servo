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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import com.netflix.servo.TagList;

public final class Metric {
    private final String name;
    private final TagList tags;
    private final long timestamp;
    private final Number value;

    public Metric(String name, TagList tags, long timestamp, Number value) {
        this.name = Preconditions.checkNotNull(name, "name cannot be null");
        this.tags = Preconditions.checkNotNull(
            tags, "tags cannot be null (name=%s)", name);
        this.timestamp = timestamp;
        this.value = Preconditions.checkNotNull(
            value, "value cannot be null (name=%s, tags=%s)", name, tags);
    }

    public String getName() {
        return name;
    }

    public TagList getTags() {
        return tags;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Number getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Metric)) {
            return false;
        }
        Metric m = (Metric) obj;
        return name.equals(m.getName())
            && tags.equals(m.getTags())
            && timestamp == m.getTimestamp()
            && value.equals(m.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(name, tags, timestamp, value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("tags", tags)
            .add("timestamp", timestamp)
            .add("value", value)
            .toString();
    }
}

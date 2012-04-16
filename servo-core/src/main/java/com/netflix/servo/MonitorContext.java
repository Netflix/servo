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
package com.netflix.servo;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Configuration settings associated with a metric.
 */
public final class MonitorContext {

    public static class Builder {
        private final String name;
        private final List<Tag> tags = new LinkedList<Tag>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder withTag(String key, String val) {
            tags.add(new BasicTag(key, val));
            return this;
        }

        public Builder withTags(TagList tagList) {
            if (tagList != null) {
                for (Tag t : tagList) {
                    tags.add(t);
                }
            }
            return this;
        }

        public Builder withTags(Collection<Tag> tagCollection) {
            tags.addAll(tagCollection);
            return this;
        }

        public MonitorContext build() {
            return new MonitorContext(this);
        }
    }

    private final String name;
    private final TagList tags;

    /**
     * Creates a new instance with a given name and tags. If {@code tags} is
     * null an empty tag list will be used.
     */
    private MonitorContext(Builder builder) {
        this.name = Preconditions.checkNotNull(builder.name, "name cannot be null");
        this.tags = (builder.tags.isEmpty()) ? SortedTagList.EMPTY : SortedTagList.builder()
                .withTags(builder.tags).build();
    }

    /**
     * Returns the name of the metric.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the tags associated with the metric.
     */
    public TagList getTags() {
        return tags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MonitorContext)) {
            return false;
        }
        MonitorContext m = (MonitorContext) obj;
        return name.equals(m.getName()) && tags.equals(m.getTags());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(name, tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("tags", tags)
                .toString();
    }
}

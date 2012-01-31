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

/**
 * Configuration settings associated with a metric.
 */
public final class MetricConfig {

    private final String name;
    private final TagList tags;

    /**
     * Creates a new instance with an empty tag list.
     */
    public MetricConfig(String name) {
        this(name, null);
    }

    /**
     * Creates a new instance with a given name and tags. If {@code tags} is
     * null an empty tag list will be used.
     */
    public MetricConfig(String name, TagList tags) {
        this.name = Preconditions.checkNotNull(name, "name cannot be null");
        this.tags = (tags == null) ? BasicTagList.EMPTY : tags;
    }

     /** Returns the name of the metric. */
    public String getName() {
        return name;
    }

     /** Returns the tags associated with the metric. */
    public TagList getTags() {
        return tags;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MetricConfig)) {
            return false;
        }
        MetricConfig m = (MetricConfig) obj;
        return name.equals(m.getName()) && tags.equals(m.getTags());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(name, tags);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("tags", tags)
            .toString();
    }
}

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
package com.netflix.servo.monitor;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.tag.Tags;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration settings associated with a monitor. A config consists of a name that is required
 * and an optional set of tags.
 */
public final class MonitorConfig {

    /** A builder to assist in creating monitor config objects. */
    public static class Builder {
        private final String name;
        private final List<Tag> tags = new LinkedList<Tag>();
        private PublishingPolicy policy = DefaultPublishingPolicy.getInstance();

        /** Create a new builder initialized with the specified config. */
        public Builder(MonitorConfig config) {
            this(config.getName());
            withTags(config.getTags());
            withPublishingPolicy(config.getPublishingPolicy());
        }

        /** Create a new builder initialized with the specified name. */
        public Builder(String name) {
            this.name = name;
        }

        /** Add a tag to the config. */
        public Builder withTag(String key, String val) {
            tags.add(Tags.newTag(key, val));
            return this;
        }

        /** Add a tag to the config. */
        public Builder withTag(Tag tag) {
            tags.add(tag);
            return this;
        }

        /** Add all tags in the list to the config. */
        public Builder withTags(TagList tagList) {
            if (tagList != null) {
                for (Tag t : tagList) {
                    tags.add(t);
                }
            }
            return this;
        }

        /** Add all tags in the list to the config. */
        public Builder withTags(Collection<Tag> tagCollection) {
            tags.addAll(tagCollection);
            return this;
        }

        /** Add the publishing policy to the config. */
        public Builder withPublishingPolicy(PublishingPolicy policy) {
            this.policy = policy;
            return this;
        }

        /** Create the monitor config object. */
        public MonitorConfig build() {
            return new MonitorConfig(this);
        }
    }

    /** Return a builder instance with the specified name. */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    private final String name;
    private final TagList tags;
    private final PublishingPolicy policy;

    /** Config is immutable, cache the hash code to improve performance. */
    private final AtomicInteger cachedHashCode = new AtomicInteger(0);

    /**
     * Creates a new instance with a given name and tags. If {@code tags} is
     * null an empty tag list will be used.
     */
    private MonitorConfig(Builder builder) {
        this.name = Preconditions.checkNotNull(builder.name, "name cannot be null");
        this.tags = (builder.tags.isEmpty())
            ? BasicTagList.EMPTY
            : new BasicTagList(builder.tags);
        this.policy = builder.policy;
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
     * Returns the publishing policy.
     */
    public PublishingPolicy getPublishingPolicy() {
        return policy;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MonitorConfig)) {
            return false;
        }
        MonitorConfig m = (MonitorConfig) obj;
        return name.equals(m.getName())
            && tags.equals(m.getTags())
            && policy.equals(m.getPublishingPolicy());
    }

    /**
     * This class is immutable so we cache the hash code after the first time it is computed. The
     * value 0 is used as an indicator that the hash code has not yet been computed, this means the
     * cache won't work for a small set of inputs, but the impact should be minimal for a decent
     * hash function. Similar technique is used for java String class.
     */
    @Override
    public int hashCode() {
        int hash = cachedHashCode.get();
        if (hash == 0) {
            hash = Objects.hashCode(name, tags, policy);
            cachedHashCode.set(hash);
        }
        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("tags", tags)
                .add("policy", policy)
                .toString();
    }

    /**
     * Returns a copy of the current MonitorConfig.
     */
    private MonitorConfig.Builder copy() {
        return MonitorConfig.builder(name).withTags(tags).withPublishingPolicy(policy);
    }

    /**
     * Returns a copy of the monitor config with an additional tag.
     */
    public MonitorConfig withAdditionalTag(Tag tag) {
        return copy().withTag(tag).build();
    }

    /**
     * Returns a copy of the monitor config with additional tags.
     */
    public MonitorConfig withAdditionalTags(TagList newTags) {
        return copy().withTags(newTags).build();
    }
}

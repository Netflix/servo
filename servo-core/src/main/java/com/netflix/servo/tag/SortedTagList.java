/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
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
package com.netflix.servo.tag;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

import java.util.*;

public final class SortedTagList implements TagList {

    public static final SortedTagList EMPTY = new Builder().build();

    private final SortedMap<String,Tag> tagSortedMap;
    private final int size;

    public static final class Builder {
        private final Map<String,Tag> data = Maps.newHashMap();

        public Builder withTags(Collection<Tag> tagsCollection) {
            for (Tag t : tagsCollection) {
                data.put(t.getKey(), t);
            }
            return this;
        }

        public Builder withTags(TagList tags){
            for (Tag t : tags) {
                data.put(t.getKey(), t);
            }
            return this;
        }

        public Builder withTag(Tag t) {
            data.put(t.getKey(), t);
            return this;
        }

        public Builder withTag(String key, String value) {
            return withTag(new BasicTag(key, value));
        }

        public SortedTagList build() {
            return new SortedTagList(this);
        }
    }

    private SortedTagList(Builder builder) {
        this.tagSortedMap = ImmutableSortedMap.copyOf(builder.data);
        this.size = tagSortedMap.size();
    }

    /**
     * Returns the tag matching a given key or null if not match is found.
     */
    @Override
    public Tag getTag(String key) {
        return tagSortedMap.get(key);
    }

    /**
     * Returns true if this list has a tag with the given key.
     */
    @Override
    public boolean containsKey(String key) {
        return tagSortedMap.containsKey(key);
    }

    /**
     * Returns true if this list is emtpy.
     */
    @Override
    public boolean isEmpty() {
        return tagSortedMap.isEmpty();
    }

    /**
     * Returns the number of tags in this list.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Tag> iterator() {
        return tagSortedMap.values().iterator();
    }

    /**
     * Returns a map containing a copy of the tags in this list.
     */
    @Override
    public Map<String, String> asMap() {
        Map<String, String> stringMap = new HashMap<String, String>(size());
        for (Tag t : tagSortedMap.values()) {
            stringMap.put(t.getKey(), t.getValue());
        }
        return stringMap;
    }

    public static Builder builder(){
        return new Builder();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SortedTagList)
                && tagSortedMap.equals(((SortedTagList) obj).tagSortedMap);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(tagSortedMap);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Joiner.on(",").join(tagSortedMap.values());
    }
}

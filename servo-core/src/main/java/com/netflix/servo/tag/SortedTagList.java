/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.tag;

import com.netflix.servo.util.Strings;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A {@link com.netflix.servo.tag.TagList} backed by a {@link SortedMap}.
 * <p/>
 * Prefer the more efficient {@link com.netflix.servo.tag.BasicTagList} implementation which
 * also provides an {@code asMap} method that returns a sorted map of tags.
 */
public final class SortedTagList implements TagList {

  /**
   * An empty {@code SortedTagList}.
   */
  public static final SortedTagList EMPTY = new Builder().build();

  private final SortedMap<String, Tag> tagSortedMap;
  private final int size;

  /**
   * Helper class to construct {@code SortedTagList} objects.
   */
  public static final class Builder {
    private final Map<String, Tag> data = new HashMap<>();

    /**
     * Add the collection of tags {@code tagsCollection} to this builder and
     * return self.
     */
    public Builder withTags(Collection<Tag> tagsCollection) {
      for (Tag tag : tagsCollection) {
        final Tag t = Tags.internCustom(tag);
        data.put(t.getKey(), t);
      }
      return this;
    }

    /**
     * Add all tags from the {@link com.netflix.servo.tag.TagList} tags to this builder
     * and return self.
     */
    public Builder withTags(TagList tags) {
      for (Tag tag : tags) {
        final Tag t = Tags.internCustom(tag);
        data.put(t.getKey(), t);
      }
      return this;
    }

    /**
     * Add the {@link Tag} to this builder and return self.
     */
    public Builder withTag(Tag tag) {
      final Tag t = Tags.internCustom(tag);
      data.put(t.getKey(), t);
      return this;
    }

    /**
     * Add the tag specified by {@code key} and {@code value} to this builder and return self.
     */
    public Builder withTag(String key, String value) {
      return withTag(Tags.newTag(key, value));
    }

    /**
     * Construct the {@code SortedTagList}.
     */
    public SortedTagList build() {
      return new SortedTagList(this);
    }
  }

  private SortedTagList(Builder builder) {
    this.tagSortedMap = Collections.unmodifiableSortedMap(
        new TreeMap<>(builder.data));
    this.size = tagSortedMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Tag getTag(String key) {
    return tagSortedMap.get(key);
  }

  /**
   * {@inheritDoc}
   */
  public String getValue(String key) {
    final Tag t = tagSortedMap.get(key);
    return (t == null) ? null : t.getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKey(String key) {
    return tagSortedMap.containsKey(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return tagSortedMap.isEmpty();
  }

  /**
   * {@inheritDoc}
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
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> asMap() {
    Map<String, String> stringMap = new LinkedHashMap<>(size);
    for (Tag t : tagSortedMap.values()) {
      stringMap.put(t.getKey(), t.getValue());
    }
    return stringMap;
  }

  /**
   * Get a new {@link com.netflix.servo.tag.SortedTagList.Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    return (obj instanceof SortedTagList)
        && tagSortedMap.equals(((SortedTagList) obj).tagSortedMap);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return tagSortedMap.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return Strings.join(",", tagSortedMap.values().iterator());
  }
}

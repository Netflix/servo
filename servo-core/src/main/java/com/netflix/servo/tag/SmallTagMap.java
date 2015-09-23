/**
 * Copyright 2014 Netflix, Inc.
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

import com.netflix.servo.util.Preconditions;
import com.netflix.servo.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This class is not intended to be used by 3rd parties and should be considered an implementation
 * detail.
 */
public class SmallTagMap implements Iterable<Tag> {
  /**
   * Max number of tags supported in a tag map. Attempting to add additional tags
   * will result in a warning logged.
   */
  public static final int MAX_TAGS = 32;

  /**
   * Initial size for the map.
   */
  public static final int INITIAL_TAG_SIZE = 8;

  private static final Logger LOGGER = LoggerFactory.getLogger(SmallTagMap.class);

  /**
   * Return a new builder to assist in creating a new SmallTagMap using the default tag size (8).
   */
  public static Builder builder() {
    return new Builder(INITIAL_TAG_SIZE);
  }

  /**
   * Helper class to build the immutable map.
   */
  public static class Builder {
    private int actualSize = 0;
    private int size;
    private Object[] buf;

    private void init(int size) {
      this.size = size;
      buf = new Object[size * 2];
      actualSize = 0;
    }

    /**
     * Create a new builder with the specified capacity.
     *
     * @param size Size of the underlying array.
     */
    public Builder(int size) {
      init(size);
    }

    /**
     * Get the number of entries in this map..
     */
    public int size() {
      return actualSize;
    }

    /**
     * True if this builder does not have any tags added to it.
     */
    public boolean isEmpty() {
      return actualSize == 0;
    }

    private void resizeIfPossible(Tag tag) {
      if (size < MAX_TAGS) {
        Object[] prevBuf = buf;
        init(size * 2);
        for (int i = 1; i < prevBuf.length; i += 2) {
          Tag t = (Tag) prevBuf[i];
          if (t != null) {
            add(t);
          }
        }
        add(tag);
      } else {
        final String msg = String.format(
            "Cannot add Tag %s - Maximum number of tags (%d) reached.",
            tag, MAX_TAGS);
        LOGGER.error(msg);
      }
    }

    /**
     * Adds a new tag to this builder.
     */
    public Builder add(Tag tag) {
      String k = tag.getKey();
      int pos = (int) (Math.abs((long) k.hashCode()) % size);
      int i = pos;
      Object ki = buf[i * 2];
      while (ki != null && !ki.equals(k)) {
        i = (i + 1) % size;
        if (i == pos) {
          resizeIfPossible(tag);
          return this;
        }
        ki = buf[i * 2];
      }

      if (ki != null) {
        buf[i * 2] = k;
        buf[i * 2 + 1] = tag;
      } else {
        if (buf[i * 2] != null) {
          throw new IllegalStateException("position has already been filled");
        }
        buf[i * 2] = k;
        buf[i * 2 + 1] = tag;
        actualSize += 1;
      }
      return this;
    }

    /**
     * Adds all tags from the {@link Iterable} tags to this builder.
     */
    public Builder addAll(Iterable<Tag> tags) {
      for (Tag tag : tags) {
        add(tag);
      }
      return this;
    }

    /**
     * Get the resulting SmallTagMap.
     */
    public SmallTagMap result() {
      Tag[] tagArray = new Tag[actualSize];
      int tagIdx = 0;

      for (int i = 1; i < buf.length; i += 2) {
        Object o = buf[i];
        if (o != null) {
          tagArray[tagIdx++] = (Tag) o;
        }
      }
      Arrays.sort(tagArray, (o1, o2) -> {
        int keyCmp = o1.getKey().compareTo(o2.getKey());
        if (keyCmp != 0) {
          return keyCmp;
        }
        return o1.getValue().compareTo(o2.getValue());
      });
      assert (tagIdx == actualSize);
      return new SmallTagMap(tagArray);
    }
  }

  private class SmallTagIterator implements Iterator<Tag> {
    private int i = 0;

    @Override
    public boolean hasNext() {
      return i < tagArray.length;
    }

    @Override
    public Tag next() {
      if (i < tagArray.length) {
        return tagArray[i++];
      }

      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("SmallTagMaps are immutable");
    }
  }

  @Override
  public Iterator<Tag> iterator() {
    return new SmallTagIterator();
  }

  private int cachedHashCode = 0;
  private final Tag[] tagArray;

  /**
   * Create a new SmallTagMap using the given array and size.
   *
   * @param tagArray sorted array of tags
   */
  SmallTagMap(Tag[] tagArray) {
    this.tagArray = Preconditions.checkNotNull(tagArray, "tagArray");
  }

  static int binarySearch(Tag[] a, String key) {
    int low = 0;
    int high = a.length - 1;

    while (low <= high) {
      final int mid = (low + high) >>> 1;
      final Tag midValTag = a[mid];
      final String midVal = midValTag.getKey();
      final int cmp = midVal.compareTo(key);
      if (cmp < 0) {
        low = mid + 1;
      } else if (cmp > 0) {
        high = mid - 1;
      } else {
        return mid; // tag key found
      }
    }
    return -(low + 1);  // tag key not found.
  }

  /**
   * Get the tag associated with a given key.
   */
  public Tag get(String key) {
    int idx = binarySearch(tagArray, key);
    if (idx < 0) {
      return null;
    } else {
      return tagArray[idx];
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    if (cachedHashCode == 0) {
      cachedHashCode = Arrays.hashCode(tagArray);
    }
    return cachedHashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "SmallTagMap{" + Strings.join(",", iterator()) + "}";
  }

  /**
   * Returns true whether this map contains a Tag with the given key.
   */
  public boolean containsKey(String k) {
    return get(k) != null;
  }

  /**
   * Returns true if this map has no entries.
   */
  public boolean isEmpty() {
    return tagArray.length == 0;
  }

  /**
   * Returns the number of Tags stored in this map.
   */
  public int size() {
    return tagArray.length;
  }

  /**
   * Returns the {@link Set} of tags.
   *
   * @deprecated This method will be removed in the next version. This is an expensive method
   * and not in the spirit of this class which is to be more efficient than the standard
   * collections library.
   */
  @Deprecated
  public Set<Tag> tagSet() {
    return new HashSet<>(Arrays.asList(tagArray));
  }

  @Override
  /** {@inheritDoc} */
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || !(obj instanceof SmallTagMap)) {
      return false;
    }

    SmallTagMap that = (SmallTagMap) obj;
    return Arrays.equals(tagArray, that.tagArray);
  }

}

/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.servo.tag;

import com.netflix.servo.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Simple immutable hash map implementation intended only for dealing with a small set of tags
 * (<= 24 in our case)
 * This class is not intended to be used by 3rd parties and should be considered an implementation
 * detail.
 * This implementation is backed by a single array and uses open addressing with
 * linear probing to resolve conflicts. Some performance tests showed that it's significantly
 * faster (2.5x)  for the BasicTagList use-case than using a LinkedHashMap as in
 * previous implementations.
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

    private volatile Set<Tag> entrySet;

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
            return new SmallTagMap(buf, actualSize);
        }
    }

    private class SmallTagIterator implements Iterator<Tag> {
        private int i = 0;
        private int pos = 0;

        @Override
        public boolean hasNext() {
            return i < dataLength;
        }

        @Override
        public Tag next() {
            while (pos < data.length && data[pos] == null) {
                pos += 2;
            }
            if (pos >= data.length) {
                throw new NoSuchElementException();
            }

            final Tag result = (Tag) data[pos + 1];
            pos += 2;
            i++;
            return result;
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
    private final Object[] data;
    private final int dataLength;

    /**
     * Create a new SmallTagMap using the given array and size.
     *
     * @param data       array with the items
     * @param dataLength number of pairs
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EI_EXPOSE_REP2",
            justification = "Only used from the builder")
    SmallTagMap(Object[] data, int dataLength) {
        this.data = data;
        this.dataLength = dataLength;
    }

    private int hash(String k) {
        final int capacity = data.length / 2;
        // casting to long because of abs(Integer.MIN_VALUE) == Integer.MIN_VALUE
        long posHashcode = Math.abs((long) k.hashCode());
        return (int) (posHashcode % capacity);
    }

    /**
     * Get the tag associated with a given key.
     */
    public Tag get(String key) {
        final int capacity = data.length / 2;
        final int pos = hash(key);
        int i = pos;
        if (!key.equals(data[i * 2])) {
            i = (i + 1) % capacity;
            while (!key.equals(data[i * 2]) && i != pos) {
                i = (i + 1) % capacity;
            }
        }
        return key.equals(data[i * 2]) ? (Tag) data[i * 2 + 1] : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (cachedHashCode == 0) {
            int hc = 0;
            for (int i = 1; i < data.length; i += 2) {
                Object o = data[i];
                if (o != null) {
                    hc += o.hashCode();
                }
            }
            cachedHashCode = hc;
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
        return dataLength == 0;
    }

    /**
     * Returns the number of Tags stored in this map.
     */
    public int size() {
        return dataLength;
    }

    /**
     * Returns the {@link Set} of tags.
     */
    public Set<Tag> tagSet() {
        if (entrySet == null) {
            entrySet = new HashSet<Tag>(dataLength);
            for (int i = 1; i < data.length; i += 2) {
                Object o = data[i];
                if (o != null) {
                    entrySet.add((Tag) o);
                }
            }
        }

        return entrySet;
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

        // quickly check lengths before the more expensive computation
        return that.dataLength == this.dataLength && tagSet().equals(that.tagSet());
    }
}

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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Immutable tag.
 */
public final class BasicTag implements Tag {

    private final String key;
    private final String value;

    /**
     * Creates a new instance with the specified key and value.
     */
    public BasicTag(String key, String value) {
        this.key = checkNotEmpty(key, "key");
        this.value = checkNotEmpty(value, "value");
    }

    /** Verify that the {@code v} is not null or an empty string. */
    private String checkNotEmpty(String v, String name) {
        Preconditions.checkNotNull(v, "%s cannot be null", name);
        Preconditions.checkArgument(!"".equals(v), "%s cannot be empty", name);
        return v;
    }

    /** {@inheritDoc} */
    public String getKey() {
        return key;
    }

    /** {@inheritDoc} */
    public String getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o instanceof BasicTag) {
            BasicTag t = (BasicTag) o;
            return key.equals(t.getKey()) && value.equals(t.getValue());
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return key + "=" + value;
    }

    /**
     * Parse a string representing a tag. A tag string should have the format
     * {@code key=value}. Whitespace at the ends of the key and value will be
     * removed. Both the key and value must have at least one character.
     *
     * @param tagString  string with encoded tag
     * @return           tag parsed from the string
     */
    public static BasicTag parseTag(String tagString) {
        String k, v;
        int eqIndex = tagString.indexOf("=");

        if (eqIndex < 0) {
            throw new IllegalArgumentException(
                "key and value must be separated by '='");
        }

        k = tagString.substring(0, eqIndex).trim();
        v = tagString.substring(eqIndex + 1, tagString.length()).trim();
        return new BasicTag(k, v);
    }

    public String tagString(){
        return toString();
    }
}

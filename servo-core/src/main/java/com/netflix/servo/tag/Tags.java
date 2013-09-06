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
package com.netflix.servo.tag;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/**
 * Helper functions for working with tags and tag lists.
 */
public final class Tags {
    /** Keep track of the strings that have been used for keys and values. */
    private static final Interner<String> STR_CACHE = Interners.newWeakInterner();

    /** Keep track of tags that have been seen before and reuse. */
    private static final Interner<Tag> TAG_CACHE = Interners.newWeakInterner();

    /** Intern strings used for tag keys or values. */
    public static String intern(String v) {
        return STR_CACHE.intern(v);
    }

    /** Returns the canonical representation of a tag. */
    public static Tag intern(Tag t) {
        return TAG_CACHE.intern(t);
    }

    /**
     * Interns custom tag types, assumes that basic tags are already interned. This is used to
     * ensure that we have a common view of tags internally. In particular, different subclasses of
     * Tag may not be equal even if they have the same key and value. Tag lists should use this to
     * ensure the equality will work as expected.
     */
    static Tag internCustom(Tag t) {
        return (t instanceof BasicTag) ? t : newTag(t.getKey(), t.getValue());
    }

    /** Create a new tag instance. */
    public static Tag newTag(String key, String value) {
        Tag newTag = new BasicTag(intern(key), intern(value));
        return intern(newTag);
    }

    /**
     * Parse a string representing a tag. A tag string should have the format {@code key=value}.
     * Whitespace at the ends of the key and value will be removed. Both the key and value must
     * have at least one character.
     *
     * @param tagString  string with encoded tag
     * @return           tag parsed from the string
     */
    public static Tag parseTag(String tagString) {
        String k, v;
        int eqIndex = tagString.indexOf("=");

        if (eqIndex < 0) {
            throw new IllegalArgumentException("key and value must be separated by '='");
        }

        k = tagString.substring(0, eqIndex).trim();
        v = tagString.substring(eqIndex + 1, tagString.length()).trim();
        return newTag(k, v);
    }

    /** Utility class. */
    private Tags() { }
}

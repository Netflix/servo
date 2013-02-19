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

import java.util.Iterator;
import java.util.Map;

/**
 * Represents a list of tags associated with a metric value.
 */
public interface TagList extends Iterable<Tag> {

    /** Returns the tag matching a given key or null if not match is found. */
    Tag getTag(String key);

    /** Returns the value matching a given key or null if not match is found. */
    String getValue(String key);

    /** Returns true if this list has a tag with the given key. */
    boolean containsKey(String key);

    /** Returns true if this list is emtpy. */
    boolean isEmpty();

    /** Returns the number of tags in this list. */
    int size();

    /** {@inheritDoc} */
    Iterator<Tag> iterator();

    /** Returns a map containing a copy of the tags in this list. */
    Map<String, String> asMap();
}

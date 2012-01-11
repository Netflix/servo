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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BasicTagList implements TagList {

    private final Map<String,Tag> tags;

    public BasicTagList(Iterable<Tag> entries) {
        ImmutableMap.Builder<String,Tag> builder = ImmutableMap.builder();
        for (Tag tag : entries) {
            builder.put(tag.getKey(), tag);
        }
        tags = builder.build();
    }
    
    public Tag getTag(String key) {
        return tags.get(key);
    }

    public boolean containsKey(String key) {
        return tags.containsKey(key);
    }

    public boolean isEmpty() {
        return tags.isEmpty();
    }

    public int size() {
        return tags.size();
    }

    public Iterator<Tag> iterator() {
        return tags.values().iterator();
    }

    public Map<String,String> asMap() {
        ImmutableMap.Builder<String,String> builder = ImmutableMap.builder();
        for (Tag tag : tags.values()) {
            builder.put(tag.getKey(), tag.getValue());
        }
        return builder.build();
    }
}

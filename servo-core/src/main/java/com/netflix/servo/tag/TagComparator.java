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

import java.io.Serializable;

import java.util.Comparator;

/**
 * Comparator for ordering tags based on the key then the value.
 */
public class TagComparator implements Comparator<Tag>, Serializable {
    /** {@inheritDoc} */
    @Override
    public int compare(Tag tag, Tag tag1) {
        if (tag.getKey().equals(tag1.getKey())) {
            return tag.getValue().compareTo(tag1.getValue());
        }

        return tag.getKey().compareTo(tag1.getKey());
    }
}

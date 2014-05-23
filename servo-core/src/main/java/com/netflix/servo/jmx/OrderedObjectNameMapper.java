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
package com.netflix.servo.jmx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.management.ObjectName;

import com.netflix.servo.monitor.Monitor;

/**
 * An {@link ObjectNameMapper} that allows the order of
 * tags to be specified when constructing the {@link ObjectName}.
 * The mapper will map the known ordered tag keys and then optionally
 * append the remaining tags. While an {@link ObjectName}'s properties
 * are meant to be unordered some visual tools such as VisualVM use the
 * given order to build a hierarchy. This ordering allows that hierarchy
 * to be manipulated.
 * <p>
 * It is recommended to always append the remaining tags to avoid collisions
 * in the generated {@link ObjectName}. The mapper remaps any characters that
 * are not alphanumeric, a period, or hypen to an underscore.
 *
 */
public final class OrderedObjectNameMapper implements ObjectNameMapper {

    private final List<String> keyOrder;
    private final boolean appendRemaining;
    private final boolean orderIncludesName;

    /**
     * Creates the mapper specifying the order of keys to use and whether
     * non-explicitly mentioned tag keys should then be appended or not to
     * the resulting {@link ObjectName}.
     * @param appendRemaining  whether to append the remaining tags
     * @param orderedKeys             the keys in order that should be used
     */
    public OrderedObjectNameMapper(boolean appendRemaining, String... orderedKeys) {
        this(appendRemaining, Arrays.asList(orderedKeys));
    }

    /**
     * Creates the mapper specifying the order of keys to use and whether
     * non-explicitly mentioned tag keys should then be appended or not to
     * the resulting {@link ObjectName}.
     * @param appendRemaining  whether to append the remaining tags
     * @param orderedKeys             the list of keys in the order that should be used
     */
    public OrderedObjectNameMapper(boolean appendRemaining, List<String> orderedKeys) {
        this.keyOrder = new ArrayList<String>(orderedKeys);
        this.appendRemaining = appendRemaining;
        this.orderIncludesName = keyOrder.contains("name");
    }

    @Override
    public ObjectName createObjectName(String domain, Monitor<?> monitor) {
        ObjectNameBuilder objBuilder = ObjectNameBuilder.forDomain(domain);
        Map<String, String> tags = new TreeMap<String, String>(
                monitor.getConfig().getTags().asMap());
        // For the known ordered keys, try to add them if they're present in the monitor's tags
        for (String knownKey : keyOrder) {
            // Special case for name as it isn't a tag
            if (knownKey.equals("name")) {
                addName(objBuilder, monitor);
            } else {
                String value = tags.remove(knownKey);
                if (value != null) {
                    objBuilder.addProperty(knownKey, value);
                }
            }
        }

        // If appending, then add the name (if not already added) and remaining tags
        if (appendRemaining) {
            if (!orderIncludesName) {
                addName(objBuilder, monitor);
            }

            for (Map.Entry<String, String> additionalTag : tags.entrySet()) {
                objBuilder.addProperty(additionalTag.getKey(), additionalTag.getValue());
            }
        }

        return objBuilder.build();
    }

    private void addName(ObjectNameBuilder builder, Monitor<?> monitor) {
        builder.addProperty("name", monitor.getConfig().getName());
    }

}

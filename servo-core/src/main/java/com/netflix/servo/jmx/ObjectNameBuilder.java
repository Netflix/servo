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
package com.netflix.servo.jmx;

import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.regex.Pattern;

/**
 * A helper class that assists in building
 * {@link ObjectName}s given monitor {@link Tag}s
 * or {@link TagList}. The builder also sanitizes
 * all values to avoid invalid input. Any characters that are
 * not alphanumeric, a period, or hypen are considered invalid
 * and are remapped to underscores.
 */
final class ObjectNameBuilder {

  private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");
  private static final Logger LOG = LoggerFactory.getLogger(ObjectNameBuilder.class);

  /**
   * Sanitizes a value by replacing any character that is not alphanumeric,
   * a period, or hyphen with an underscore.
   *
   * @param value the value to sanitize
   * @return the sanitized value
   */
  public static String sanitizeValue(String value) {
    return INVALID_CHARS.matcher(value).replaceAll("_");
  }

  /**
   * Creates an {@link ObjectNameBuilder} given the JMX domain.
   *
   * @param domain the JMX domain
   * @return The ObjectNameBuilder
   */
  public static ObjectNameBuilder forDomain(String domain) {
    return new ObjectNameBuilder(domain);
  }

  private final StringBuilder nameStrBuilder;

  private ObjectNameBuilder(String domain) {
    nameStrBuilder = new StringBuilder(sanitizeValue(domain));
    nameStrBuilder.append(":");
  }

  /**
   * Adds the {@link TagList} as {@link ObjectName} properties.
   *
   * @param tagList the tag list to add
   * @return This builder
   */
  public ObjectNameBuilder addProperties(TagList tagList) {
    for (Tag tag : tagList) {
      addProperty(tag);
    }

    return this;
  }

  /**
   * Adds the {@link Tag} as a {@link ObjectName} property.
   *
   * @param tag the tag to add
   * @return This builder
   */
  public ObjectNameBuilder addProperty(Tag tag) {
    return addProperty(tag.getKey(), tag.getValue());
  }

  /**
   * Adds the key/value as a {@link ObjectName} property.
   *
   * @param key   the key to add
   * @param value the value to add
   * @return This builder
   */
  public ObjectNameBuilder addProperty(String key, String value) {
    nameStrBuilder.append(sanitizeValue(key))
        .append('=')
        .append(sanitizeValue(value)).append(",");
    return this;
  }

  /**
   * Builds the {@link ObjectName} given the configuration.
   *
   * @return The created ObjectName
   */
  public ObjectName build() {
    final String name = nameStrBuilder.substring(0, nameStrBuilder.length() - 1);
    try {
      return new ObjectName(name);
    } catch (MalformedObjectNameException e) {
      LOG.warn("Invalid ObjectName provided: " + name);
      throw Throwables.propagate(e);
    }
  }

}

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

/**
 * Helper functions for working with tags and tag lists.
 */
public final class Tags {
  /**
   * Interns custom tag types, assumes that basic tags are already interned. This is used to
   * ensure that we have a common view of tags internally. In particular, different subclasses of
   * Tag may not be equal even if they have the same key and value. Tag lists should use this to
   * ensure the equality will work as expected.
   */
  static Tag internCustom(Tag t) {
    return (t instanceof BasicTag) ? t : newTag(t.getKey(), t.getValue());
  }

  /**
   * Create a new tag instance.
   */
  public static Tag newTag(String key, String value) {
    return new BasicTag(key, value);
  }

  /**
   * Parse a string representing a tag. A tag string should have the format {@code key=value}.
   * Whitespace at the ends of the key and value will be removed. Both the key and value must
   * have at least one character.
   *
   * @param tagString string with encoded tag
   * @return tag parsed from the string
   */
  public static Tag parseTag(String tagString) {
    String k;
    String v;
    int eqIndex = tagString.indexOf("=");

    if (eqIndex < 0) {
      throw new IllegalArgumentException("key and value must be separated by '='");
    }

    k = tagString.substring(0, eqIndex).trim();
    v = tagString.substring(eqIndex + 1, tagString.length()).trim();
    return newTag(k, v);
  }

  /**
   * Utility class.
   */
  private Tags() {
  }
}

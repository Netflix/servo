/*
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
package com.netflix.servo.util;

import java.util.Iterator;

/**
 * Static helpers for {@code String} instances.
 */
public final class Strings {
  private Strings() {
  }

  /**
   * Returns true if the given string is null or is the empty string.
   */
  public static boolean isNullOrEmpty(String string) {
    return string == null || string.isEmpty();
  }

  /**
   * Join the string representation of each part separated by the given separator string.
   *
   * @param separator Separator string. For example ","
   * @param parts     An iterator of the parts to join
   * @return The string formed by joining each part separated by the given separator.
   */
  public static String join(String separator, Iterator<?> parts) {
    Preconditions.checkNotNull(separator, "separator");
    Preconditions.checkNotNull(parts, "parts");

    StringBuilder builder = new StringBuilder();
    if (parts.hasNext()) {
      builder.append(parts.next().toString());
      while (parts.hasNext()) {
        builder.append(separator);
        builder.append(parts.next().toString());
      }
    }
    return builder.toString();
  }
}

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
package com.netflix.servo.util;

/**
 * Internal convenience methods that help a method or constructor check whether it was invoked
 * correctly. Please notice that this should be considered an internal implementation detail, and
 * it is subject to change without notice.
 */
public final class Preconditions {
  private Preconditions() {
  }

  /**
   * Ensures the object reference is not null.
   */
  public static <T> T checkNotNull(T obj, String name) {
    if (obj == null) {
      String msg = String.format("parameter '%s' cannot be null", name);
      throw new NullPointerException(msg);
    }
    return obj;
  }

    /**
     * Ensures the truth of an expression involving one or more parameters to the
     * calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
  public static void checkArgument(boolean expression, String errorMessage) {
    checkArgument(expression, errorMessage, null);
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the
   * calling method.
   *
   *
   * @param expression a boolean expression
   * @param errorMessage the error message that can be a formattable string
   * @param args arguments if using a formatted string
   * @throws IllegalArgumentException if {@code expression} is false
   */
  public static void checkArgument(boolean expression, String errorMessage, String... args) {
    if (!expression) {
      if (args != null && args.length > 0) {
        String message = String.format(errorMessage, args);
        throw new IllegalArgumentException(message);
      } else {
        throw new IllegalArgumentException(errorMessage);
      }
    }
  }
}

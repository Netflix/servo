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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for dealing with Iterables. For internal use of servo only.
 */
public final class Iterables {
  private Iterables() {
  }

  /**
   * Creates a new {@link Iterable} by concatenating two iterables.
   */
  public static <E> Iterable<E> concat(Iterable<E> a, Iterable<E> b) {
    List<E> result = new ArrayList<>();
    for (E e : a) {
      result.add(e);
    }
    for (E e : b) {
      result.add(e);
    }

    return result;
  }
}

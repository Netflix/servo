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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Utility class to create umodifiable views for lists.
 */
public final class UnmodifiableList {
  private UnmodifiableList() {
  }

  /**
   * Returns an unmodifiable view of the list composed of elements.
   *
   * @param elements Array of elements.
   * @param <E>      Type of the elements of the list.
   * @return an unmodifiable view of the list composed of elements.
   */
  @SafeVarargs
  public static <E> List<E> of(E... elements) {
    Preconditions.checkNotNull(elements, "elements");
    return Collections.unmodifiableList(Arrays.asList(elements));
  }

  /**
   * Returns an unmodifiable view of the list composed of elements.
   *
   * @param elements Array of elements.
   * @param <E>      Type of the elements of the list.
   * @return an unmodifiable view of the list composed of elements.
   */
  public static <E> List<E> copyOf(E[] elements) {
    Preconditions.checkNotNull(elements, "elements");
    List<E> result = new ArrayList<>(elements.length);
    Collections.addAll(result, elements);
    return Collections.unmodifiableList(result);
  }

  // hack to simplify casting
  static <T> Collection<T> cast(Iterable<T> iterable) {
    return (Collection<T>) iterable;
  }

  /**
   * Returns an unmodifiable view of the list composed of elements.
   *
   * @param elements Iterable of elements.
   * @param <E>      Type of the elements of the list.
   * @return an unmodifiable view of the list composed of elements.
   */
  public static <E> List<E> copyOf(Iterable<? extends E> elements) {
    Preconditions.checkNotNull(elements, "elements");
    List<E> result = (elements instanceof Collection)
        // can pre-allocate the array
        ? new ArrayList<>(cast(elements).size())
        // cannot
        : new ArrayList<>();
    for (E e : elements) {
      result.add(e);
    }
    return Collections.unmodifiableList(result);
  }
}

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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility class to create unmodifiable sets.
 */
public final class UnmodifiableSet {
  private UnmodifiableSet() {
  }

  /**
   * Returns an unmodifiable view of the set created from the given elements.
   *
   * @param elements Array of elements
   * @param <E>      type of the elements
   * @return an unmodifiable view of the set created from the given elements.
   */
  @SafeVarargs
  public static <E> Set<E> of(E... elements) {
    Set<E> result = new HashSet<>();
    Collections.addAll(result, elements);
    return Collections.unmodifiableSet(result);
  }

  /**
   * Returns an unmodifiable view of the set created from the given elements.
   *
   * @param elementsIterator iterator to get the elements of the set.
   * @param <E>              type of the elements
   * @return an unmodifiable view of the set created from the given elements.
   */
  public static <E> Set<E> copyOf(Iterator<? extends E> elementsIterator) {
    Set<E> result = new HashSet<>();
    while (elementsIterator.hasNext()) {
      result.add(elementsIterator.next());
    }
    return Collections.unmodifiableSet(result);
  }
}

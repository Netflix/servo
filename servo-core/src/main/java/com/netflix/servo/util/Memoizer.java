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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A class that can cache the results of computations for a given amount of time.
 *
 * @param <T> Type of the value cached
 */
public final class Memoizer<T> {
  /**
   * Create a memoizer that caches the value produced by getter for a given duration.
   *
   * @param getter   A {@link Callable} that returns a new value. This should not throw.
   * @param duration how long we should keep the cached value before refreshing it.
   * @param unit     unit of time for {@code duration}
   * @param <T>      type of the value produced by the {@code getter}
   * @return A thread safe memoizer.
   */
  public static <T> Memoizer<T> create(Callable<T> getter, long duration, TimeUnit unit) {
    return new Memoizer<>(getter, duration, unit);
  }

  private volatile long whenItExpires = 0L;
  private volatile T value;
  private final Callable<T> getter;
  private final long durationNanos;

  private Memoizer(Callable<T> getter, long duration, TimeUnit unit) {
    this.durationNanos = unit.toNanos(duration);
    this.getter = getter;
  }

  /**
   * Get or refresh and return the latest value.
   */
  public T get() {
    long expiration = whenItExpires;
    long now = System.nanoTime();

    // if uninitialized or expired update value
    if (expiration == 0 || now >= expiration) {
      synchronized (this) {
        // ensure a different thread didn't update it
        if (whenItExpires == expiration) {
          whenItExpires = now + durationNanos;
          try {
            value = getter.call();
          } catch (Exception e) {
            // shouldn't happen
            throw Throwables.propagate(e);
          }
        }
      }
    }
    return value;
  }
}

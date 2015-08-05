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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple utility class to create thread factories.
 */
public final class ThreadFactories {
  private ThreadFactories() {
  }

  private static final ThreadFactory BACKING_FACTORY = Executors.defaultThreadFactory();

  /**
   * Create a new {@link ThreadFactory} that produces daemon threads with a given name format.
   *
   * @param fmt String format: for example foo-%d
   * @return a new {@link ThreadFactory}
   */
  public static ThreadFactory withName(final String fmt) {
    return new ThreadFactory() {
      private final AtomicLong count = new AtomicLong(0);

      @Override
      public Thread newThread(Runnable r) {
        final Thread t = BACKING_FACTORY.newThread(r);
        t.setDaemon(true);
        t.setName(String.format(fmt, count.getAndIncrement()));
        return t;
      }
    };
  }
}

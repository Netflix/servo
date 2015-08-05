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
package com.netflix.servo.monitor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.netflix.servo.util.Memoizer;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.netflix.servo.annotations.DataSourceType.COUNTER;
import static com.netflix.servo.annotations.DataSourceType.GAUGE;

/**
 * Wraps a cache to provide common metrics.
 */
class MonitoredCache {

  private static final int CACHE_TIME = 10;

  /**
   * When polling metrics each monitor gets called independently. If we call cache.stats directly
   * each monitor call will create a new stats object. This supplier is used to control the calls
   * for updated stats so that typically it will only need to be done once per sampling interval
   * for all exposed monitors.
   */
  private final Memoizer<CacheStats> memoStats;

  MonitoredCache(final Cache<?, ?> cache) {
    final Callable<CacheStats> supplier = cache::stats;
    memoStats = Memoizer.create(supplier, CACHE_TIME, TimeUnit.SECONDS);
  }

  @com.netflix.servo.annotations.Monitor(name = "averageLoadPenalty", type = GAUGE)
  double averageLoadPenalty() {
    return memoStats.get().averageLoadPenalty();
  }

  @com.netflix.servo.annotations.Monitor(name = "evictionCount", type = COUNTER)
  long evictionCount() {
    return memoStats.get().evictionCount();
  }

  @com.netflix.servo.annotations.Monitor(name = "hitCount", type = COUNTER)
  long hitCount() {
    return memoStats.get().hitCount();
  }

  @com.netflix.servo.annotations.Monitor(name = "loadCount", type = COUNTER)
  long loadCount() {
    return memoStats.get().loadCount();
  }

  @com.netflix.servo.annotations.Monitor(name = "loadExceptionCount", type = COUNTER)
  long loadExceptionCount() {
    return memoStats.get().loadExceptionCount();
  }

  @com.netflix.servo.annotations.Monitor(name = "loadSuccessCount", type = COUNTER)
  long loadSuccessCount() {
    return memoStats.get().loadSuccessCount();
  }

  @com.netflix.servo.annotations.Monitor(name = "missCount", type = COUNTER)
  long missCount() {
    return memoStats.get().missCount();
  }

  @com.netflix.servo.annotations.Monitor(name = "requestCount", type = COUNTER)
  long requestCount() {
    return memoStats.get().requestCount();
  }

  @com.netflix.servo.annotations.Monitor(name = "totalLoadTime", type = COUNTER)
  long totalLoadTime() {
    return memoStats.get().totalLoadTime();
  }
}

/**
 * Copyright 2013 Netflix, Inc.
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

import static com.netflix.servo.annotations.DataSourceType.*;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;

import java.util.concurrent.TimeUnit;

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
    private final Supplier<CacheStats> statsSupplier;

    MonitoredCache(final Cache<?, ?> cache) {
        final Supplier<CacheStats> supplier = new Supplier<CacheStats>() {
            public CacheStats get() {
                return cache.stats();
            }
        };
        statsSupplier = Suppliers.memoizeWithExpiration(supplier, CACHE_TIME, TimeUnit.SECONDS);
    }

    @com.netflix.servo.annotations.Monitor(name = "averageLoadPenalty", type = GAUGE)
    double averageLoadPenalty() {
        return statsSupplier.get().averageLoadPenalty();
    }

    @com.netflix.servo.annotations.Monitor(name = "evictionCount", type = COUNTER)
    long evictionCount() {
        return statsSupplier.get().evictionCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "hitCount", type = COUNTER)
    long hitCount() {
        return statsSupplier.get().hitCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "loadCount", type = COUNTER)
    long loadCount() {
        return statsSupplier.get().loadCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "loadExceptionCount", type = COUNTER)
    long loadExceptionCount() {
        return statsSupplier.get().loadExceptionCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "loadSuccessCount", type = COUNTER)
    long loadSuccessCount() {
        return statsSupplier.get().loadSuccessCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "missCount", type = COUNTER)
    long missCount() {
        return statsSupplier.get().missCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "requestCount", type = COUNTER)
    long requestCount() {
        return statsSupplier.get().requestCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "totalLoadTime", type = COUNTER)
    long totalLoadTime() {
        return statsSupplier.get().totalLoadTime();
    }
}

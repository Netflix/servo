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

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Wraps a thread pool to provide common metrics.
 */
class MonitoredThreadPool {

    private final ThreadPoolExecutor pool;

    MonitoredThreadPool(ThreadPoolExecutor pool) {
        this.pool = pool;
    }

    @com.netflix.servo.annotations.Monitor(name = "activeCount", type = GAUGE)
    int getActiveCount() {
        return pool.getActiveCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "completedTaskCount", type = COUNTER)
    long getCompletedTaskCount() {
        return pool.getCompletedTaskCount();
    }

    @com.netflix.servo.annotations.Monitor(name = "corePoolSize", type = GAUGE)
    int getCorePoolSize() {
        return pool.getCorePoolSize();
    }

    @com.netflix.servo.annotations.Monitor(name = "maximumPoolSize", type = GAUGE)
    int getMaximumPoolSize() {
        return pool.getMaximumPoolSize();
    }

    @com.netflix.servo.annotations.Monitor(name = "poolSize", type = GAUGE)
    int getPoolSize() {
        return pool.getPoolSize();
    }

    @com.netflix.servo.annotations.Monitor(name = "queueSize", type = GAUGE)
    int getQueueSize() {
        return pool.getQueue().size();
    }

    @com.netflix.servo.annotations.Monitor(name = "taskCount", type = COUNTER)
    long getTaskCount() {
        return pool.getTaskCount();
    }
}

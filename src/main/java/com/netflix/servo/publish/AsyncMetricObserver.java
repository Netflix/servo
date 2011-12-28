/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.publish;

import com.netflix.servo.jmx.DataSourceType;
import com.netflix.servo.jmx.Monitor;
import com.netflix.servo.jmx.MonitorId;

import com.google.common.base.Preconditions;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps another observer and asynchronously updates it in the background. The
 * update call will always return immediately. If the queue fills up newer
 * updates will overwrite older updates.
 *
 * If an exception is thrown when calling update on wrapped observer it will
 * be logged, but otherwise ignored.
 */
public final class AsyncMetricObserver implements MetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(AsyncMetricObserver.class);

    @MonitorId
    private final String mName;
    
    @Monitor(name="UpdateCount", type=DataSourceType.COUNTER,
             description="Total number of times update has been called on "
                        +"the wrapped observer.")
    private final AtomicInteger mUpdates = new AtomicInteger(0);
    
    @Monitor(name="UpdateFailureCount", type=DataSourceType.COUNTER,
             description="Number of times the update call on the wrapped "
                        +"observer failed with an exception.")
    private final AtomicInteger mFailedUpdates = new AtomicInteger(0);

    private final MetricObserver mObserver;

    private final int mQueueSize;
    private final Queue<List<Metric>> mQueue;

    private final Thread mUpdateThread;

    public AsyncMetricObserver(
            String name, MetricObserver observer, int queueSize) {
        mName = Preconditions.checkNotNull(name);
        mObserver = Preconditions.checkNotNull(observer);
        mQueueSize = queueSize;
        Preconditions.checkArgument(queueSize >= 1,
            "invalid queueSize %d, size must be >= 1", queueSize);

        mQueue = new ArrayDeque<List<Metric>>(queueSize);

        String threadName = getClass().getSimpleName() + "-" + mName;
        mUpdateThread = new Thread(new UpdateTask(), threadName);
        mUpdateThread.setDaemon(true);
        mUpdateThread.start();
    }

    public void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        synchronized (mQueue) {
            if (mQueue.size() == mQueueSize) {
                mQueue.poll();
            }
            mQueue.offer(metrics);
            mQueue.notify();
        }
    }

    private void nextUpdate() {
        List<Metric> metrics = null;
        synchronized (mQueue) {
            while (mQueue.isEmpty()) {
                try {
                    mQueue.wait();
                } catch (InterruptedException e) {
                    LOGGER.trace(e.getMessage(), e);
                }
            }
            metrics = mQueue.poll();
        }
        try {
            mObserver.update(metrics);
        } catch (Throwable t) {
            LOGGER.warn("update failed for downstream queue", t);
            mFailedUpdates.incrementAndGet();
        } finally {
            mUpdates.incrementAndGet();
        }
    }

    private class UpdateTask implements Runnable {
        public void run() {
            while (true) {
                nextUpdate();
            }
        }
    }
}

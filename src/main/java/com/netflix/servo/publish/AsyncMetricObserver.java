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

import com.google.common.base.Preconditions;

import java.util.List;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

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
public class AsyncMetricObserver extends BaseMetricObserver {

    private static final Logger log = LoggerFactory.getLogger(AsyncMetricObserver.class);

    private final MetricObserver wrappedObserver;

    private final int updateQueueSize;
    private final long expireTime;
    private final BlockingQueue<TimestampedUpdate> updateQueue;

    private final Thread updateProcessingThread;

    public AsyncMetricObserver(String name, MetricObserver observer, int queueSize, long expireTime) {
        super(name);
        this.expireTime = expireTime;
        wrappedObserver = Preconditions.checkNotNull(observer);
        updateQueueSize = queueSize;
        Preconditions.checkArgument(queueSize >= 1,
            "invalid queueSize %d, size must be >= 1", queueSize);

        updateQueue = new LinkedBlockingDeque<TimestampedUpdate>(queueSize);

        String threadName = getClass().getSimpleName() + "-" + this.name;
        updateProcessingThread = new Thread(new UpdateProcessor(), threadName);
        updateProcessingThread.setDaemon(true);
        updateProcessingThread.start();
    }

    public void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        synchronized (updateQueue) {
            TimestampedUpdate update = new TimestampedUpdate(System.currentTimeMillis(), metrics);
            boolean result = updateQueue.offer(update);
            while(!result){
                updateQueue.remove();
                result = updateQueue.offer(update);
            }
        }
    }

    private void processUpdate() {
        List<Metric> metrics = null;
        synchronized (updateQueue) {
            metrics = updateQueue.remove().getMetrics();
        }
        try {
            wrappedObserver.update(metrics);
        } catch (Throwable t) {
            log.warn("update failed for downstream queue", t);
            failedUpdateCount.incrementAndGet();
        } finally {
            updateCount.incrementAndGet();
        }
    }

    private class UpdateProcessor implements Runnable {
        public void run() {
            while (true) {
                processUpdate();
            }
        }
    }
    
    private class TimestampedUpdate{
        private final long timestamp;
        private final List<Metric> metrics;

        public TimestampedUpdate(long timestamp, List<Metric> metrics){
            this.timestamp = timestamp;
            this.metrics = metrics;
        }

        long getTimestamp(){
            return timestamp;
        }

        List<Metric> getMetrics(){
            return metrics;
        }
    }
}

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
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps another observer and asynchronously updates it in the background. The
 * update call will always return immediately. If the queue fills up newer
 * updates will overwrite older updates.
 * <p/>
 * If an exception is thrown when calling update on wrapped observer it will
 * be logged, but otherwise ignored.
 */
public final class AsyncMetricObserver extends BaseMetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(AsyncMetricObserver.class);

    private final MetricObserver wrappedObserver;

    private final long expireTime;
    private final BlockingQueue<TimestampedUpdate> updateQueue;

    private final Thread updateProcessingThread;

    @Monitor(name="UpdateExpiredCount", type=DataSourceType.COUNTER,
             description="Number of updates that expire in queue.")
    private final AtomicInteger expiredUpdateCount = new AtomicInteger(0);

    /**
     * Creates a new instance.
     *
     * @param name        name of this observer
     * @param observer    a wrapped observer that will be updated asynchronously
     * @param queueSize   maximum size of the update queue, if the queue fills
     *                    up older entries will be dropped
     * @param expireTime  age in milliseconds before an update expires and will
     *                    not be passed on to the wrapped observer
     */
    public AsyncMetricObserver(
            String name,
            MetricObserver observer,
            int queueSize,
            long expireTime) {
        super(name);
        this.expireTime = expireTime;
        wrappedObserver = Preconditions.checkNotNull(observer);
        Preconditions.checkArgument(queueSize >= 1,
                "invalid queueSize %d, size must be >= 1", queueSize);

        updateQueue = new LinkedBlockingDeque<TimestampedUpdate>(queueSize);

        String threadName = getClass().getSimpleName() + "-" + name;
        updateProcessingThread = new Thread(new UpdateProcessor(), threadName);
        updateProcessingThread.setDaemon(true);
        updateProcessingThread.start();
    }

    /**
     * Creates a new instance with an unbounded queue and no expiration time.
     *
     * @param name        name of this observer
     * @param observer    a wrapped observer that will be updated asynchronously
     */
    public AsyncMetricObserver(String name, MetricObserver observer) {
        this(name, observer, Integer.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * Creates a new instance with no expiration time.
     *
     * @param name        name of this observer
     * @param observer    a wrapped observer that will be updated asynchronously
     * @param queueSize   maximum size of the update queue, if the queue fills
     *                    up older entries will be dropped
     */
    public AsyncMetricObserver(
            String name, MetricObserver observer, int queueSize) {
        this(name, observer, queueSize, Long.MAX_VALUE);
    }

    /** {@inheritDoc} */
    public void updateImpl(List<Metric> metrics) {
        long now = System.currentTimeMillis();
        TimestampedUpdate update = new TimestampedUpdate(now, metrics);
        boolean result = updateQueue.offer(update);
        while (!result) {
            updateQueue.remove();
            result = updateQueue.offer(update);
        }
    }

    private void processUpdate() {
        TimestampedUpdate update;
        try {
            update = updateQueue.take();

            long cutoff = System.currentTimeMillis() - expireTime;
            if (update.getTimestamp() < cutoff) {
                expiredUpdateCount.incrementAndGet();
                return;
            }

            wrappedObserver.update(update.getMetrics());
        } catch (InterruptedException ie){
            LOGGER.warn("interrupted while adding to queue, update dropped");
            incrementFailedCount();
        } catch (Throwable t) {
            LOGGER.warn("update failed for downstream queue", t);
            incrementFailedCount();
        }
    }

    private class UpdateProcessor implements Runnable {
        public void run() {
            while (true) {
                processUpdate();
            }
        }
    }

    private static class TimestampedUpdate {
        private final long timestamp;
        private final List<Metric> metrics;

        public TimestampedUpdate(long timestamp, List<Metric> metrics) {
            this.timestamp = timestamp;
            this.metrics = metrics;
        }

        long getTimestamp() {
            return timestamp;
        }

        List<Metric> getMetrics() {
            return metrics;
        }
    }
}

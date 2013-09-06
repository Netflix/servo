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

import com.netflix.servo.DefaultMonitorRegistry;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for managing a set of AtomicLong instances mapped to a particular step interval.
 * The current implementation keeps an array of with two items where one is the current value
 * being updated and the other is the value from the previous interval and is only available for
 * polling.
 */
class StepLong {

    private static final Counter REPOLLED_INTERVALS = newCounter("servo.monitor.repolledIntervals");
    private static final Counter POLLED_INTERVALS = newCounter("servo.monitor.polledIntervals");
    private static final Counter MISSED_INTERVALS = newCounter("servo.monitor.missedIntervals");

    private static Counter newCounter(String name) {
        Counter c = new BasicCounter(MonitorConfig.builder(name).build());
        DefaultMonitorRegistry.getInstance().register(c);
        return c;
    }

    private final long step;
    private final long init;
    private final Clock clock;

    private final AtomicLong[] data;

    private final AtomicLong lastPollTime = new AtomicLong(0L);

    private final AtomicLong lastInitPos = new AtomicLong(0L);

    StepLong(long step, long init, Clock clock) {
        Preconditions.checkArgument(step >= 1000L, "minimum step size is 1 second");
        this.step = step;
        this.init = init;
        this.clock = clock;
        data = new AtomicLong[] {
            new AtomicLong(init), new AtomicLong(init)
        };
    }

    AtomicLong current() {
        final long now = clock.now();
        final long stepTime = now / step;
        final int pos = (int) (stepTime % 2);
        final long v = data[pos].get();
        final long lastInit = lastInitPos.get();
        if (lastInit != stepTime && lastInitPos.compareAndSet(lastInit, stepTime)) {
            data[pos].compareAndSet(v, init);
        }
        return data[pos];
    }

    Datapoint poll() {
        final long now = clock.now();
        final long stepTime = now / step;
        final long value = data[(int) ((stepTime + 1) % 2)].getAndSet(init);

        final long last = lastPollTime.getAndSet(now);
        final long missed = (now - last) / step - 1;

        if (last / step == now / step) {
            REPOLLED_INTERVALS.increment();
            return new Datapoint(now / step * step, value);
        } else if (last > 0L && missed > 0L) {
            MISSED_INTERVALS.increment(missed);
            return Datapoint.UNKNOWN;
        } else {
            POLLED_INTERVALS.increment();
            return new Datapoint(now / step * step, value);
        }
    }
}


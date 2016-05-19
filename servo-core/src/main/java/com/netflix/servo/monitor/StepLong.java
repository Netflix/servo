/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

import com.netflix.servo.util.Clock;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for managing a set of AtomicLong instances mapped to a particular step interval.
 * The current implementation keeps an array of two items where one is the current value
 * being updated and the other is the value from the previous interval and is only available for
 * polling.
 */
class StepLong {
  private static final int PREVIOUS = 0;
  private static final int CURRENT = 1;

  private final long init;
  private final Clock clock;

  private final AtomicLong[] data;
  private final AtomicLong[] lastInitPos;

  StepLong(long init, Clock clock) {
    this.init = init;
    this.clock = clock;
    lastInitPos = new AtomicLong[Pollers.NUM_POLLERS];
    for (int i = 0; i < Pollers.NUM_POLLERS; ++i) {
      lastInitPos[i] = new AtomicLong(0L);
    }
    data = new AtomicLong[2 * Pollers.NUM_POLLERS];
    for (int i = 0; i < data.length; ++i) {
      data[i] = new AtomicLong(init);
    }
  }

  void addAndGet(long amount) {
    for (int i = 0; i < Pollers.NUM_POLLERS; ++i) {
      getCurrent(i).addAndGet(amount);
    }
  }

  private void rollCount(int pollerIndex, long now) {
    final long step = Pollers.POLLING_INTERVALS[pollerIndex];
    final long stepTime = now / step;
    final long lastInit = lastInitPos[pollerIndex].get();
    if (lastInit < stepTime && lastInitPos[pollerIndex].compareAndSet(lastInit, stepTime)) {
      final int prev = 2 * pollerIndex + PREVIOUS;
      final int curr = 2 * pollerIndex + CURRENT;
      final long v = data[curr].getAndSet(init);
      // Need to check if there was any activity during the previous step interval. If there was
      // then the init position will move forward by 1, otherwise it will be older. No activity
      // means the previous interval should be set to the `init` value.
      data[prev].set((lastInit == stepTime - 1) ? v : init);
    }
  }

  AtomicLong getCurrent(int pollerIndex) {
    rollCount(pollerIndex, clock.now());
    return data[2 * pollerIndex + CURRENT];
  }

  long poll(int pollerIndex) {
    rollCount(pollerIndex, clock.now());

    final int prevPos = 2 * pollerIndex + PREVIOUS;
    return data[prevPos].get();
  }

  @Override
  public String toString() {
    return "StepLong{init=" + init
        + ", data=" + Arrays.toString(data)
        + ", lastInitPos=" + Arrays.toString(lastInitPos) + '}';
  }
}


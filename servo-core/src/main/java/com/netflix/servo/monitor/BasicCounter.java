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

import com.netflix.servo.annotations.DataSourceType;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple counter implementation backed by an {@link java.util.concurrent.atomic.AtomicLong}.
 * The value is the total count for the life of the counter. Observers are responsible
 * for converting to a rate and handling overflows if they occur.
 */
public final class BasicCounter extends AbstractMonitor<Number> implements Counter {
  private final AtomicLong count = new AtomicLong();

  /**
   * Creates a new instance of the counter.
   */
  public BasicCounter(MonitorConfig config) {
    super(config.withAdditionalTag(DataSourceType.COUNTER));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void increment() {
    count.incrementAndGet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void increment(long amount) {
    count.getAndAdd(amount);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Number getValue(int pollerIdx) {
    return count.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof BasicCounter)) {
      return false;
    }
    BasicCounter m = (BasicCounter) obj;
    return config.equals(m.getConfig()) && count.get() == m.count.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = config.hashCode();
    long n = count.get();
    result = 31 * result + (int) (n ^ (n >>> 32));
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "BasicCounter{config=" + config + ", count=" + count.get() + '}';
  }
}

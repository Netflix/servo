/**
 * Copyright 2014 Netflix, Inc.
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
import com.netflix.servo.util.ClockWithOffset;
import com.netflix.servo.util.UnmodifiableList;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A Monitor for tracking a longer operation that might last for many minutes or hours. For tracking
 * frequent calls that last less than a polling interval (usually one minute) please use a
 * {@link com.netflix.servo.monitor.BasicTimer} instead.
 * <p/>
 * This monitor will create two gauges:
 * <ul>
 * <li>A duration: will report the current duration in seconds.
 * (defined as the sum of the time of all active tasks.)</li>
 * <li>Number of active tasks.</li>
 * </ul>
 * <p/>
 * The names for the monitors will be the base name passed to the constructor plus a
 * suffix of .duration and .activeTasks respectively.
 */
public class DurationTimer extends AbstractMonitor<Long> implements CompositeMonitor<Long> {

  private final List<Monitor<?>> monitors;
  private final AtomicLong nextTaskId = new AtomicLong(0L);
  private final ConcurrentMap<Long, Long> tasks = new ConcurrentHashMap<>();
  private final Clock clock;

  private static MonitorConfig subId(MonitorConfig config, String sub) {
    String newName = config.getName() + "." + sub;
    return MonitorConfig.builder(newName).withTags(config.getTags())
        .withPublishingPolicy(config.getPublishingPolicy())
        .build();
  }

  /**
   * Create a new DurationTimer using the provided configuration.
   */
  public DurationTimer(MonitorConfig config) {
    this(config, ClockWithOffset.INSTANCE);
  }

  /**
   * Create a new DurationTimer using a specific configuration and clock. This is useful for
   * unit tests that need to manipulate the clock.
   */
  public DurationTimer(MonitorConfig config, final Clock clock) {
    super(config);

    this.clock = clock;

    Monitor<?> duration = new BasicGauge<>(subId(config, "duration"),
        () -> getDurationMillis() / 1000L);

    Monitor<?> activeTasks = new BasicGauge<>(subId(config, "activeTasks"),
        new Callable<Long>() {
          @Override
          public Long call() throws Exception {
            return (long) tasks.size();
          }
        });

    monitors = UnmodifiableList.of(duration, activeTasks);
  }

  private long getDurationMillis() {
    long now = clock.now();
    long sum = 0L;
    for (long startTime : tasks.values()) {
      sum += now - startTime;
    }
    return Math.max(sum, 0L);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getValue() {
    return getValue(0);
  }

  @Override
  public Long getValue(int pollerIndex) {
    return getDurationMillis() / 1000;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Monitor<?>> getMonitors() {
    return monitors;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DurationTimer that = (DurationTimer) o;
    return getConfig().equals(that.getConfig())
        && nextTaskId.get() == that.nextTaskId.get()
        && tasks.equals(that.tasks);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = getConfig().hashCode();
    long id = nextTaskId.get();
    result = 31 * result + (int) (id ^ (id >>> 32));
    result = 31 * result + tasks.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "DurationTimer{config=" + getConfig()
        + ", tasks=" + tasks
        + ", monitors=" + monitors
        + ", nextTaskId=" + nextTaskId.get()
        + '}';
  }

  /**
   * Returns a stopwatch that has been started and will automatically
   * record its result to this timer when stopped. Every time this method is called
   * the number of active tasks for the timer will be incremented.
   * The number will be decremented when the stopwatch is stopped.
   */
  public Stopwatch start() {
    Stopwatch s = new DurationStopwatch();
    s.start();
    return s;
  }

  private class DurationStopwatch implements Stopwatch {
    private long id = -1L;

    @Override
    public void start() {
      this.id = nextTaskId.getAndIncrement();
      tasks.put(id, clock.now());
    }

    @Override
    public void stop() {
      if (id >= 0) {
        tasks.remove(id);
        id = -1L;
      }
    }

    @Override
    public void reset() {
      if (id >= 0) {
        tasks.put(id, clock.now());
      }
    }

    @Override
    public long getDuration(TimeUnit timeUnit) {
      long durationMs = 0L;
      if (id >= 0) {
        long start = tasks.get(id);
        durationMs = clock.now() - start;
      }
      durationMs = Math.max(0L, durationMs);
      return timeUnit.convert(durationMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public long getDuration() {
      return getDuration(TimeUnit.SECONDS);
    }
  }
}

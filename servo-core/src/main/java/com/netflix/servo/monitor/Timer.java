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

import java.util.concurrent.TimeUnit;

/**
 * Monitor type for tracking how much time something is taking.
 */
public interface Timer extends NumericMonitor<Long> {

  /**
   * Returns a stopwatch that has been started and will automatically
   * record its result to this timer when stopped.
   */
  Stopwatch start();

  /**
   * The time unit reported by this timer.
   */
  TimeUnit getTimeUnit();

  /**
   * Record a new value for this timer.
   *
   * @deprecated Use record(duration, timeUnit). By always providing a timeUnit to record()
   * you can have a base time unit of seconds, but
   * use recordings with timeunit of milliseconds for example.
   */
  @Deprecated
  void record(long duration);

  /**
   * Record a new value that was collected with the given TimeUnit.
   */
  void record(long duration, TimeUnit timeUnit);
}

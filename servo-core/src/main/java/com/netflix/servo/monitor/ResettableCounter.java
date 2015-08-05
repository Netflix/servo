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


/**
 * Counter implementation that keeps track of updates since the last reset.
 * This class will be removed in the next release. Use a StepCounter directly
 * if you specifically need the functionality previously provided by this class.
 *
 * @deprecated Use Monitors.newCounter() instead to get a default implementation
 */
@Deprecated
public class ResettableCounter extends StepCounter {
  /**
   * Creates a new instance. Prefer a {@link com.netflix.servo.monitor.StepCounter}
   */
  public ResettableCounter(MonitorConfig config) {
    super(config);
  }

  /**
   * Creates a new instance configured for a given polling interval. Note that the 'l' parameter
   * is ignored. The functionality has been replaced by {@link com.netflix.servo.monitor.Pollers}
   * and {@link com.netflix.servo.monitor.StepCounter}.
   * <p/>
   * Prefer a {@link com.netflix.servo.monitor.StepCounter}
   */
  public ResettableCounter(MonitorConfig config, long l) {
    super(config);
  }
}

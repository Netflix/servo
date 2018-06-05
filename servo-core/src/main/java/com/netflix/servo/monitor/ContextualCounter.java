/*
 * Copyright 2011-2018 Netflix, Inc.
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

import com.google.common.base.Function;
import com.netflix.servo.tag.TaggingContext;

/**
 * Composite that maintains separate simple counters for each distinct set of tags returned by the
 * tagging context.
 */
public class ContextualCounter extends AbstractContextualMonitor<Number, Counter>
    implements Counter {

  /**
   * Create a new instance of the counter.
   *
   * @param config     shared configuration
   * @param context    provider for context specific tags
   * @param newMonitor function to create new counters
   */
  public ContextualCounter(
      MonitorConfig config,
      TaggingContext context,
      Function<MonitorConfig, Counter> newMonitor) {
    super(config, context, newMonitor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void increment() {
    getMonitorForCurrentContext().increment();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void increment(long amount) {
    getMonitorForCurrentContext().increment(amount);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Number getValue(int pollerIndex) {
    return getMonitorForCurrentContext().getValue(pollerIndex);
  }
}

/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.servo.publish;

import com.netflix.servo.Metric;
import com.netflix.servo.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for simple pollers that do not benefit from filtering in advance.
 * Sub-classes implement {@link #pollImpl} to return a list and all filtering
 * will be taken care of by the provided implementation of {@link #poll}.
 */
public abstract class BaseMetricPoller implements MetricPoller {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Return a list of all current metrics for this poller.
   */
  public abstract List<Metric> pollImpl(boolean reset);

  /**
   * {@inheritDoc}
   */
  public final List<Metric> poll(MetricFilter filter) {
    return poll(filter, false);
  }

  /**
   * {@inheritDoc}
   */
  public final List<Metric> poll(MetricFilter filter, boolean reset) {
    Preconditions.checkNotNull(filter, "filter");
    List<Metric> metrics = pollImpl(reset);
    List<Metric> retained = metrics.stream().filter(m -> filter.matches(m.getConfig()))
        .collect(Collectors.toList());
    logger.debug("received {} metrics, retained {} metrics", metrics.size(), retained.size());

    return Collections.unmodifiableList(retained);
  }
}

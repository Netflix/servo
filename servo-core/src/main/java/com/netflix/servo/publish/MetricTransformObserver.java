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
package com.netflix.servo.publish;

import com.netflix.servo.Metric;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An observer that will transform the list of metrics using a given function.
 */
public class MetricTransformObserver implements MetricObserver {
  private final Function<Metric, Metric> transformer;
  private final MetricObserver observer;

  /**
   * Create a new MetricTransformObserver using the given transfomer function.
   *
   * @param transformer The function used to transform metrics.
   * @param observer    The MetricObserver that will receive the transfomed metrics.
   */
  public MetricTransformObserver(Function<Metric, Metric> transformer, MetricObserver observer) {
    this.transformer = transformer;
    this.observer = observer;
  }

  @Override
  public void update(List<Metric> metrics) {
    List<Metric> transformed = metrics.stream()
        .map(transformer::apply).collect(Collectors.toList());
    observer.update(transformed);
  }

  @Override
  public String getName() {
    return "MetricTransformObserver";
  }
}

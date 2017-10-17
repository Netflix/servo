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

/**
 * A poller that can be used to fetch current values for a list of metrics on
 * demand.
 */
public interface MetricPoller {
  /**
   * Fetch the current values for a set of metrics that match the provided
   * filter. This method should be cheap, thread-safe, and interruptible so
   * that it can be called frequently to collect metrics at a regular
   * interval.
   *
   * @param filter retricts the set of metrics
   * @return list of current metric values
   */
  List<Metric> poll(MetricFilter filter);

  /**
   * Fetch the current values for a set of metrics that match the provided
   * filter. This method should be cheap, thread-safe, and interruptible so
   * that it can be called frequently to collect metrics at a regular
   * interval.
   *
   * @param filter retricts the set of metrics
   * @param reset  ignored. This is kept for backwards compatibility only.
   * @return list of current metric values
   */
  List<Metric> poll(MetricFilter filter, boolean reset);

  /**
   * Try to convert an object into a number. Boolean values will return 1 if
   * true and 0 if false. If the value is null or an unknown data type null
   * will be returned.
   */
  static Number asNumber(Object value) {
    Number num = null;
    if (value == null) {
      num = null;
    } else if (value instanceof Number) {
      num = (Number) value;
    } else if (value instanceof Boolean) {
      num = ((Boolean) value) ? 1 : 0;
    }
    return num;
  }
}

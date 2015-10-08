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

import com.google.common.util.concurrent.AtomicDouble;

/**
 * A {@link Gauge} that reports a double value.
 */
public class DoubleGauge extends NumberGauge {
  private final AtomicDouble number;

  /**
   * Create a new instance with the specified configuration.
   *
   * @param config configuration for this gauge
   */
  public DoubleGauge(MonitorConfig config) {
    super(config);
    number = new AtomicDouble(0.0);
    setBackingNumber(number);
  }

  /**
   * Set the current value.
   */
  public void set(Double n) {
    number.set(n);
  }

  /**
   * Returns a reference to the {@link com.google.common.util.concurrent.AtomicDouble}.
   */
  public AtomicDouble getNumber() {
    return number;
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

    DoubleGauge that = (DoubleGauge) o;

    return getConfig().equals(that.getConfig())
        && getValue().equals(that.getValue());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = config.hashCode();
    final int n = Double.valueOf(number.get()).hashCode();
    result = 31 * result + n;
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Number getValue(int pollerIdx) {
    // we return the actual value at the time of the call and not a reference
    // to the atomic number so the value doesn't change and is also available to jmx viewers
    return number.get();
  }
}

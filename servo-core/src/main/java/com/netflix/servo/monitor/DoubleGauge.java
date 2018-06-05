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

import com.google.common.util.concurrent.AtomicDouble;
import com.netflix.servo.SpectatorContext;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.tag.TagList;
import com.netflix.spectator.api.Id;

/**
 * A {@link Gauge} that reports a double value.
 */
public class DoubleGauge extends AbstractMonitor<Double>
    implements Gauge<Double>, SpectatorMonitor {

  private final MonitorConfig baseConfig;
  private final AtomicDouble number;
  private final SpectatorContext.LazyGauge spectatorGauge;

  /**
   * Create a new instance with the specified configuration.
   *
   * @param config configuration for this gauge
   */
  public DoubleGauge(MonitorConfig config) {
    super(config.withAdditionalTag(DataSourceType.GAUGE));
    baseConfig = config;
    number = new AtomicDouble(0.0);
    spectatorGauge = SpectatorContext.gauge(config);
  }

  /**
   * Set the current value.
   */
  public void set(Double n) {
    spectatorGauge.set(n);
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
  public void initializeSpectator(TagList tags) {
    Id id = SpectatorContext.createId(baseConfig.withAdditionalTags(tags));
    spectatorGauge.setId(id);
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
  public Double getValue(int pollerIdx) {
    // we return the actual value at the time of the call and not a reference
    // to the atomic number so the value doesn't change and is also available to jmx viewers
    return number.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "DoubleGauge{config=" + config + ", number=" + number + '}';
  }
}

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

import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple informational implementation that maintains a string value.
 */
public final class BasicInformational extends AbstractMonitor<String> implements Informational {
  private final AtomicReference<String> info = new AtomicReference<>();

  /**
   * Creates a new instance of the counter.
   */
  public BasicInformational(MonitorConfig config) {
    super(config.withAdditionalTag(DataSourceType.INFORMATIONAL));
  }

  /**
   * Set the value to show for this monitor.
   */
  public void setValue(String value) {
    info.set(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getValue(int pollerIndex) {
    return info.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof BasicInformational)) {
      return false;
    }
    BasicInformational that = (BasicInformational) o;

    String thisInfo = info.get();
    String thatInfo = that.info.get();
    return config.equals(that.config)
        && (thisInfo == null ? thatInfo == null : thisInfo.equals(thatInfo));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = config.hashCode();
    int infoHashcode = info.get() != null ? info.get().hashCode() : 0;
    result = 31 * result + infoHashcode;
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "BasicInformational{config=" + config + ", info=" + info + '}';
  }
}

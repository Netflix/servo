/**
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.servo.monitor;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.netflix.servo.annotations.DataSourceType;

public class NumberGauge extends AbstractMonitor<Number> implements Gauge<Number>  {
    private final Number number;

    public NumberGauge(MonitorConfig config, Number number) {
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        Preconditions.checkNotNull(number);
        this.number = number;
    }

    @Override
    public Number getValue(int pollerIdx) {
        return number;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof NumberGauge)) {
            return false;
        }

        final NumberGauge that = (NumberGauge) o;
        return config.equals(that.config) && number.equals(that.number);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(number, config);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("number", number).add("config", config).toString();
    }
}

/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.servo.publish.atlas;

import com.fasterxml.jackson.core.JsonGenerator;
import com.netflix.servo.Metric;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.util.Objects;
import com.netflix.servo.util.Preconditions;

import java.io.IOException;

/**
 * A metric that can be reported to Atlas.
 */
class AtlasMetric implements JsonPayload {

  private final MonitorConfig config;
  private final long step;
  private final long start;
  private final double value;

  AtlasMetric(Metric m, long step) {
    this(m.getConfig(), step, m.getTimestamp(), m.getNumberValue());
  }

  AtlasMetric(MonitorConfig config, long step, long start, Number value) {
    this.config = Preconditions.checkNotNull(config, "config");
    this.step = step;
    this.start = start;
    this.value = Preconditions.checkNotNull(value, "value").doubleValue();
  }

  MonitorConfig getConfig() {
    return config;
  }

  long getStep() {
    return step;
  }

  long getStartTime() {
    return start;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AtlasMetric)) {
      return false;
    }
    AtlasMetric m = (AtlasMetric) obj;
    return config.equals(m.getConfig())
        && step == m.getStep()
        && start == m.getStartTime()
        && Double.compare(value, m.value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(config, step, start, value);
  }

  @Override
  public String toString() {
    return "AtlasMetric{config=" + config + ", step=" + step
        + ", start=" + start + ", value=" + value + '}';
  }

  @Override
  public void toJson(JsonGenerator gen) throws IOException {
    gen.writeStartObject();

    gen.writeObjectFieldStart("tags");
    gen.writeStringField("name", config.getName());
    for (Tag tag : config.getTags()) {
      gen.writeStringField(tag.getKey(), tag.getValue());
    }
    gen.writeEndObject();

    gen.writeNumberField("step", step);
    gen.writeNumberField("start", start);
    gen.writeNumberField("value", value);

    gen.writeEndObject();
    gen.flush();
  }
}

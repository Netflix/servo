/**
 * Copyright 2015 Netflix, Inc.
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
  private final long start;
  private final double value;

  AtlasMetric(Metric m) {
    this(m.getConfig(), m.getTimestamp(), m.getNumberValue());
  }

  AtlasMetric(MonitorConfig config, long start, Number value) {
    this.config = Preconditions.checkNotNull(config, "config");
    this.value = Preconditions.checkNotNull(value, "value").doubleValue();
    this.start = start;
  }

  MonitorConfig getConfig() {
    return config;
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
        && start == m.getStartTime()
        && Double.compare(value, m.value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(config, start, value);
  }

  @Override
  public String toString() {
    return "AtlasMetric{config=" + config
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

    gen.writeNumberField("start", start);
    gen.writeNumberField("value", value);

    gen.writeEndObject();
    gen.flush();
  }
}

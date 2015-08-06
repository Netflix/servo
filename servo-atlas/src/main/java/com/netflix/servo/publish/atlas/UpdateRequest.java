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
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.Objects;
import com.netflix.servo.util.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Request sent to the atlas-publish API.
 */
public final class UpdateRequest implements JsonPayload {

  private final TagList tags;
  private final List<AtlasMetric> metrics;

  /**
   * Create an UpdateRequest to be sent to atlas.
   *
   * @param tags          common tags for all metrics in the request.
   * @param metricsToSend Array of metrics to send.
   * @param numMetrics    How many metrics in the array metricsToSend should be sent. Note
   *                      that this value needs to be lower or equal to metricsToSend.length
   */
  public UpdateRequest(TagList tags, Metric[] metricsToSend, int numMetrics) {
    Preconditions.checkArgument(metricsToSend.length > 0, "metricsToSend is empty");
    Preconditions.checkArgument(numMetrics > 0 && numMetrics <= metricsToSend.length,
        "numMetrics is 0 or out of bounds");

    this.metrics = new ArrayList<>(numMetrics);
    for (int i = 0; i < numMetrics; ++i) {
      Metric m = metricsToSend[i];
      if (m.hasNumberValue()) {
        metrics.add(new AtlasMetric(m));
      }
    }

    this.tags = tags;
  }

  TagList getTags() {
    return tags;
  }

  List<AtlasMetric> getMetrics() {
    return metrics;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof UpdateRequest)) {
      return false;
    }
    UpdateRequest req = (UpdateRequest) obj;
    return tags.equals(req.getTags())
        && metrics.equals(req.getMetrics());
  }

  @Override
  public int hashCode() {
    return Objects.hash(tags, metrics);
  }

  @Override
  public String toString() {
    return "UpdateRequest{tags=" + tags + ", metrics=" + metrics + '}';
  }

  @Override
  public void toJson(JsonGenerator gen) throws IOException {
    gen.writeStartObject();

    // common tags
    gen.writeObjectFieldStart("tags");
    for (Tag tag : tags) {
      gen.writeStringField(
          ValidCharacters.toValidCharset(tag.getKey()),
          ValidCharacters.toValidCharset(tag.getValue()));
    }
    gen.writeEndObject();

    gen.writeArrayFieldStart("metrics");
    for (AtlasMetric m : metrics) {
      m.toJson(gen);
    }
    gen.writeEndArray();

    gen.writeEndObject();
    gen.flush();
  }
}

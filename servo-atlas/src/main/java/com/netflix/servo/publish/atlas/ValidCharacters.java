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

import com.netflix.servo.Metric;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class to deal with rewriting keys/values to the character set accepted by atlas.
 */
public final class ValidCharacters {
  /**
   * Only allow letters, numbers, underscores, dashes and dots in our identifiers.
   */
  private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");

  private ValidCharacters() {
    // utility class
  }

  /**
   * Convert a given string to one where all characters are valid.
   */
  public static String toValidCharset(String str) {
    return INVALID_CHARS.matcher(str).replaceAll("_");
  }

  /**
   * Return a new metric where the name and all tags are using the valid character
   * set.
   */
  public static Metric toValidValue(Metric metric) {
    MonitorConfig cfg = metric.getConfig();
    MonitorConfig.Builder cfgBuilder = MonitorConfig.builder(toValidCharset(cfg.getName()));
    for (Tag orig : cfg.getTags()) {
      cfgBuilder.withTag(toValidCharset(orig.getKey()), toValidCharset(orig.getValue()));
    }
    cfgBuilder.withPublishingPolicy(cfg.getPublishingPolicy());
    return new Metric(cfgBuilder.build(), metric.getTimestamp(), metric.getValue());
  }

  /**
   * Create a new list of metrics where all metrics are using the valid character set.
   */
  public static List<Metric> toValidValues(List<Metric> metrics) {
    List<Metric> fixedMetrics = new ArrayList<Metric>(metrics.size());
    for (Metric m : metrics) {
      fixedMetrics.add(toValidValue(m));
    }
    return fixedMetrics;
  }
}

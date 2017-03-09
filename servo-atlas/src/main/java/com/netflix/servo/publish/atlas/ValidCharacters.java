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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class to deal with rewriting keys/values to the character set accepted by atlas.
 */
public final class ValidCharacters {
  /**
   * Only allow letters, numbers, underscores, dashes and dots in our identifiers.
   */

  private static final boolean[] CHARS_ALLOWED = new boolean[128];
  private static final boolean[] CHARS_ALLOWED_GROUPS = new boolean[128];

  static {
    CHARS_ALLOWED['.'] = true;
    CHARS_ALLOWED['-'] = true;
    CHARS_ALLOWED['_'] = true;
    for (char ch = '0'; ch <= '9'; ch++) {
      CHARS_ALLOWED[ch] = true;
    }
    for (char ch = 'A'; ch <= 'Z'; ch++) {
      CHARS_ALLOWED[ch] = true;
    }
    for (char ch = 'a'; ch <= 'z'; ch++) {
      CHARS_ALLOWED[ch] = true;
    }

    // relax rules a bit for tags describing groups
    System.arraycopy(CHARS_ALLOWED, 0, CHARS_ALLOWED_GROUPS, 0, CHARS_ALLOWED.length);
    CHARS_ALLOWED_GROUPS['^'] = true;
    CHARS_ALLOWED_GROUPS['~'] = true;
  }

  private ValidCharacters() {
    // utility class
  }

  private static boolean hasInvalidCharactersTable(boolean[] table, String str) {
    final int n = str.length();
    for (int i = 0; i < n; i++) {
      final char c = str.charAt(i);
      if (c >= table.length || !table[c]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check whether a given string contains an invalid character.
   */
  public static boolean hasInvalidCharacters(String str) {
    return hasInvalidCharactersTable(CHARS_ALLOWED, str);
  }

  private static String toValidCharsetTable(boolean[] table, String str) {
    if (hasInvalidCharactersTable(table, str)) {
      final int n = str.length();
      final StringBuilder buf = new StringBuilder(n + 1);
      for (int i = 0; i < n; i++) {
        final char c = str.charAt(i);
        if (c < table.length && table[c]) {
          buf.append(c);
        } else {
          buf.append('_');
        }
      }
      return buf.toString();
    } else {
      return str;
    }
  }

  /**
   * Convert a given string to one where all characters are valid.
   */
  public static String toValidCharset(String str) {
    return toValidCharsetTable(CHARS_ALLOWED, str);
  }

  private static final List<String> RELAXED_GROUP_KEYS = Arrays.asList("nf.asg", "nf.cluster");

  /**
   * Return a new metric where the name and all tags are using the valid character
   * set.
   */
  public static Metric toValidValue(Metric metric) {
    MonitorConfig cfg = metric.getConfig();
    MonitorConfig.Builder cfgBuilder = MonitorConfig.builder(toValidCharset(cfg.getName()));
    for (Tag orig : cfg.getTags()) {
      final String key = orig.getKey();
      if (RELAXED_GROUP_KEYS.contains(key)) {
        cfgBuilder.withTag(key, toValidCharsetTable(CHARS_ALLOWED_GROUPS, orig.getValue()));
      } else {
        cfgBuilder.withTag(toValidCharset(key), toValidCharset(orig.getValue()));
      }
    }
    cfgBuilder.withPublishingPolicy(cfg.getPublishingPolicy());
    return new Metric(cfgBuilder.build(), metric.getTimestamp(), metric.getValue());
  }

  /**
   * Create a new list of metrics where all metrics are using the valid character set.
   */
  public static List<Metric> toValidValues(List<Metric> metrics) {
    return metrics.stream().map(ValidCharacters::toValidValue).collect(Collectors.toList());
  }

  /**
   * Serialize a tag to the given JsonGenerator.
   */
  public static void tagToJson(JsonGenerator gen, Tag tag) throws IOException {
    final String key = tag.getKey();
    if (RELAXED_GROUP_KEYS.contains(key)) {
      gen.writeStringField(key, toValidCharsetTable(CHARS_ALLOWED_GROUPS, tag.getValue()));
    } else {
      gen.writeStringField(toValidCharset(tag.getKey()), toValidCharset(tag.getValue()));
    }
  }
}

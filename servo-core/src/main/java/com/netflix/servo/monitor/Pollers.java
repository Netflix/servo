/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.servo.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Poller configuration. This class provides the mechanism
 * to know how many pollers will be used, and at their estimated polling intervals.
 */
public final class Pollers {
  private Pollers() {
  }

  /**
   * A comma separated list of longs indicating the frequency of the pollers. For example: <br/>
   * {@code 60000, 10000 }<br/>
   * indicates that the main poller runs every 60s and a secondary
   * poller will run every 10 seconds.
   * This is used to deal with monitors that need to get reset after they're polled.
   * For example a MinGauge or a MaxGauge.
   */
  public static final String POLLERS = System.getProperty("servo.pollers", "60000,10000");
  static final long[] DEFAULT_PERIODS = new long[]{60000L, 10000L};

  /**
   * Polling intervals in milliseconds.
   */
  static final long[] POLLING_INTERVALS = parse(POLLERS);

  private static final List<Long> POLLING_INTERVALS_AS_LIST;

  /**
   * Get list of polling intervals in milliseconds.
   */
  public static List<Long> getPollingIntervals() {
    return POLLING_INTERVALS_AS_LIST;
  }

  /**
   * Number of pollers that will run.
   */
  public static final int NUM_POLLERS = POLLING_INTERVALS.length;

  /**
   * For debugging. Simple toString for non-empty arrays
   */
  private static String join(long[] a) {
    assert (a.length > 0);
    StringBuilder builder = new StringBuilder();
    builder.append(a[0]);
    for (int i = 1; i < a.length; ++i) {
      builder.append(',');
      builder.append(a[i]);
    }
    return builder.toString();
  }

  /**
   * Parse the content of the system property that describes the polling intervals,
   * and in case of errors
   * use the default of one poller running every minute.
   */
  static long[] parse(String pollers) {
    String[] periods = pollers.split(",\\s*");
    long[] result = new long[periods.length];

    boolean errors = false;
    Logger logger = LoggerFactory.getLogger(Pollers.class);
    for (int i = 0; i < periods.length; ++i) {
      String period = periods[i];
      try {
        result[i] = Long.parseLong(period);
        if (result[i] <= 0) {
          logger.error("Invalid polling interval: {} must be positive.", period);
          errors = true;
        }
      } catch (NumberFormatException e) {
        logger.error("Cannot parse '{}' as a long: {}", period, e.getMessage());
        errors = true;
      }
    }

    if (errors || periods.length == 0) {
      logger.info("Using a default configuration for poller intervals: {}",
          join(DEFAULT_PERIODS));
      return DEFAULT_PERIODS;
    } else {
      return result;
    }
  }

  static {
    List<Long> intervals = new ArrayList<>(POLLING_INTERVALS.length);
    for (Long interval : POLLING_INTERVALS) {
      intervals.add(interval);
    }
    POLLING_INTERVALS_AS_LIST = Collections.unmodifiableList(intervals);
  }
}

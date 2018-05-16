/*
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.servo.publish.apache;

import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.publish.BaseMetricPoller;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;
import com.netflix.servo.util.UnmodifiableList;
import com.netflix.servo.util.UnmodifiableSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for simple pollers that do not benefit from filtering in advance.
 * Sub-classes implement {@link #pollImpl} to return a list and all filtering
 * will be taken care of by the provided implementation of {@link #poll}.
 */
public class ApacheStatusPoller extends BaseMetricPoller {

  private final StatusFetcher fetcher;
  private static final List<Metric> EMPTY_LIST = Collections.emptyList();

  /**
   * Mechanism used to fetch a status page.
   */
  public interface StatusFetcher {
    /**
     * Return the apache status page as an {@link InputStream}.
     */
    InputStream fetchStatus() throws IOException;
  }

  /**
   * Simple class to fetch a status page using the {@link java.net.URL} class.
   */
  public static class URLStatusFetcher implements StatusFetcher {
    private final URL url;

    /**
     * Fetch an apache status page using the given URL passed as a String.
     */
    public URLStatusFetcher(String url) throws MalformedURLException {
      this(new URL(url));
    }

    /**
     * Fetch an apache status page using the given URL.
     */
    public URLStatusFetcher(URL url) {
      this.url = url;
    }

    @Override
    public InputStream fetchStatus() throws IOException {
      final URLConnection con = url.openConnection();
      con.setConnectTimeout(1000);
      con.setReadTimeout(2000);
      return con.getInputStream();
    }
  }

  private static final class StatusPageParserUtil {
    private StatusPageParserUtil() {
      // utility class
    }

    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");
    private static final Pattern STAT_LINE = Pattern.compile("^([^:]+): ([.\\d]+)$");
    private static final Pattern SCOREBOARD_LINE = Pattern.compile("^Scoreboard: (\\S+)$");
    private static final char[] SCOREBOARD_CHARS = {
        '_', 'S', 'R', 'W', 'K', 'D', 'C', 'L', 'G', 'I', '.', '*'};
    private static final Tag CLASS_TAG = Tags.newTag("class", "ApacheStatusPoller");
    /**
     * Metrics that should be included.
     */
    private static final Set<String> WHITELISTED_METRICS = UnmodifiableSet.of("Total_Accesses",
        "Total_kBytes", "Uptime",
        "BusyWorkers", "IdleWorkers",
        "ConnsTotal", "ConnsAsyncWriting",
        "ConnsAsyncKeepAlive", "ConnsAsyncClosing");
    private static final int ASCII_CHARS = 128;
    private static final String SCOREBOARD = "Scoreboard";

    private static String getScoreboardName(char c) {
      switch (c) {
        case '_':
          return "WaitingForConnection";
        case 'S':
          return "StartingUp";
        case 'R':
          return "ReadingRequest";
        case 'W':
          return "SendingReply";
        case 'K':
          return "Keepalive";
        case 'D':
          return "DnsLookup";
        case 'C':
          return "ClosingConnection";
        case 'L':
          return "Logging";
        case 'G':
          return "GracefullyFinishing";
        case 'I':
          return "IdleCleanupOfWorker";
        case '.':
          return "OpenSlotWithNoCurrentProcess";
        default:
          return "UnknownState";
      }
    }

    static List<Metric> parseStatLine(String line, long timestamp) {
      final Matcher m = STAT_LINE.matcher(line);
      if (!m.matches()) {
        return EMPTY_LIST;
      }

      final String name = INVALID_CHARS.matcher(m.group(1)).replaceAll("_");
      if (!WHITELISTED_METRICS.contains(name)) {
        return EMPTY_LIST;
      }
      final double value = Double.parseDouble(m.group(2));


      final Tag metricType = (name.startsWith("Total") || name.startsWith("Uptime"))
          ? DataSourceType.COUNTER : DataSourceType.GAUGE;
      final MonitorConfig monitorConfig = MonitorConfig.builder(name)
          .withTag(metricType)
          .withTag(CLASS_TAG)
          .build();
      Metric metric = new Metric(monitorConfig, timestamp, value);
      return UnmodifiableList.of(metric);
    }

    /*
     * "_" Waiting for Connection,
     * "S" Starting up,
     * "R" Reading Request,
     * "W" Sending Reply,
     * "K" Keepalive (read),
     * "D" DNS Lookup,
     * "C" Closing connection,
     * "L" Logging,
     * "G" Gracefully finishing,
     * "I" Idle cleanup of worker,
     * ." Open slot with no current process (ignored)
     */
    static List<Metric> parseScoreboardLine(String line, long timestamp) {
      final Matcher m = SCOREBOARD_LINE.matcher(line);
      if (!m.matches()) {
        return EMPTY_LIST;
      }

      final char[] scoreboard = m.group(1).toCharArray();

      final double[] tally = new double[ASCII_CHARS];
      for (final char item : SCOREBOARD_CHARS) {
        tally[item] = 0;
      }
      for (final char item : scoreboard) {
        final int idx = item % ASCII_CHARS;
        tally[idx]++;
      }

      final List<Metric> scoreboardMetrics = new ArrayList<>();
      for (final char item : SCOREBOARD_CHARS) {
        if (item == '.') { // Open slots are not particularly useful to track
          continue;
        }
        final double value = tally[item];
        final String state = getScoreboardName(item);
        final MonitorConfig monitorConfig = MonitorConfig.builder(SCOREBOARD)
            .withTag(DataSourceType.GAUGE)
            .withTag(CLASS_TAG)
            .withTag("state", state)
            .build();
        final Metric metric = new Metric(monitorConfig, timestamp, value);
        scoreboardMetrics.add(metric);
      }
      return Collections.unmodifiableList(scoreboardMetrics);
    }

    static List<Metric> parse(InputStream input, long timestamp) throws IOException {
      final List<Metric> metrics = new ArrayList<>();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
        String line = reader.readLine();
        while (line != null) {
          if (line.startsWith(SCOREBOARD)) {
            metrics.addAll(parseScoreboardLine(line, timestamp));
          } else {
            metrics.addAll(parseStatLine(line, timestamp));
          }
          line = reader.readLine();
        }
      }
      return Collections.unmodifiableList(metrics);
    }
  }

  /**
   * Create a new ApacheStatusPoller with a given mechanism to fetch the status page.
   *
   * @param fetcher The {@link StatusFetcher} that will be used to refresh the metrics.
   */
  public ApacheStatusPoller(StatusFetcher fetcher) {
    super();
    this.fetcher = fetcher;
  }

  List<Metric> pollImpl(long timestamp) {
    try {
      try (InputStream statusStream = fetcher.fetchStatus()) {
        return StatusPageParserUtil.parse(statusStream, timestamp);
      }
    } catch (IOException e) {
      logger.error("Could not fetch status page", e);
      return EMPTY_LIST;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Metric> pollImpl(boolean reset) {
    return pollImpl(System.currentTimeMillis());
  }
}

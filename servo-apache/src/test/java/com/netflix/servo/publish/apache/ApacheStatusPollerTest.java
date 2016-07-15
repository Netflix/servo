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
import com.netflix.servo.tag.Tag;
import com.netflix.servo.util.UnmodifiableList;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class ApacheStatusPollerTest {

  private static final int OPEN_SLOTS = 20430;
  private static final long TIMESTAMP = 1234L;
  private static final int TOTAL_ACCESSES = 3020567;
  private static final int KBYTES = 46798223;
  private static final int UPTIME = 3204689;
  private static final double RPS = .942546;
  private static final double BPS = 14953.5;
  private static final int BPR = 15865;
  private static final int BUSY_WORKERS = 5;
  private static final int IDLE_WORKERS = 45;

  private static String repeat(char c, int n) {
    char[] result = new char[n];
    Arrays.fill(result, c);
    return new String(result);
  }

  private static Metric metric(String name, double value, Tag metricType) {
    MonitorConfig config = MonitorConfig.builder(name).withTag(metricType)
        .withTag("class", "ApacheStatusPoller").build();
    return new Metric(config, TIMESTAMP, value);
  }

  private static Metric scoreboard(String state, double value) {
    MonitorConfig config = MonitorConfig.builder("Scoreboard")
        .withTag(DataSourceType.GAUGE)
        .withTag("state", state)
        .withTag("class", "ApacheStatusPoller").build();
    return new Metric(config, TIMESTAMP, value);
  }

  private static Metric counter(String name, double value) {
    return metric(name, value, DataSourceType.COUNTER);
  }

  private static Metric gauge(String name, double value) {
    return metric(name, value, DataSourceType.GAUGE);
  }

  @Test
  public void testParse() throws Exception {
    final String statusText = "Total Accesses: " + TOTAL_ACCESSES + "\n"
        + "Total kBytes: " + KBYTES + "\n"
        + "CPULoad: .044688\n"
        + "Uptime: " + UPTIME + "\n"
        + "ReqPerSec: " + RPS + "\n"
        + "BytesPerSec: " + BPS + "\n"
        + "BytesPerReq: " + BPR + "\n"
        + "BusyWorkers: " + BUSY_WORKERS + "\n"
        + "IdleWorkers: " + IDLE_WORKERS + "\n"
        + "Scoreboard: __________________K___W_K_____K______________K____"
        + repeat('.', OPEN_SLOTS)
        + "\n";
    ApacheStatusPoller.StatusFetcher fetcher = () -> new ByteArrayInputStream(statusText.getBytes("UTF-8"));

    ApacheStatusPoller poller = new ApacheStatusPoller(fetcher);

    List<Metric> metrics = poller.pollImpl(TIMESTAMP);

    Metric accesses = counter("Total_Accesses", TOTAL_ACCESSES);
    Metric kBytes = counter("Total_kBytes", KBYTES);
    Metric uptime = counter("Uptime", UPTIME);

    List<Metric> counters = UnmodifiableList.of(accesses, kBytes, uptime);
    Metric busyWorkers = gauge("BusyWorkers", BUSY_WORKERS);
    Metric idleWorkers = gauge("IdleWorkers", IDLE_WORKERS);
    List<Metric> gauges = UnmodifiableList.of(busyWorkers, idleWorkers);

    Metric waitingForConnection = scoreboard("WaitingForConnection", 45.0);
    Metric startingUp = scoreboard("StartingUp", 0.0);
    Metric readingRequest = scoreboard("ReadingRequest", 0.0);
    Metric sendingReply = scoreboard("SendingReply", 1.0);
    Metric keepalive = scoreboard("Keepalive", 4.0);
    Metric dnsLookup = scoreboard("DnsLookup", 0.0);
    Metric closingConnection = scoreboard("ClosingConnection", 0.0);
    Metric logging = scoreboard("Logging", 0.0);
    Metric gracefullyFinishing = scoreboard("GracefullyFinishing", 0.0);
    Metric idleCleanupOfWorker = scoreboard("IdleCleanupOfWorker", 0.0);
    Metric unknownState = scoreboard("UnknownState", 0.0);
    List<Metric> scoreboard = UnmodifiableList.of(waitingForConnection,
        startingUp, readingRequest, sendingReply, keepalive, dnsLookup, closingConnection,
        logging, gracefullyFinishing, idleCleanupOfWorker, unknownState);

    List<Metric> expected = new ArrayList<>();
    expected.addAll(counters);
    expected.addAll(gauges);
    expected.addAll(scoreboard);

    assertEquals(metrics, expected);
  }

  @Test
  public void testParseNewFormat() throws Exception {
    final String statusText = "localhost\n"
        + "ServerVersion: Apache/2.4.16 (Ubuntu)\n"
        + "ServerMPM: worker\n"
        + "Server Built: 2015-09-08T00:00:00\n"
        + "CurrentTime: Tuesday, 08-Sep-2015 21:07:08 UTC\n"
        + "RestartTime: Tuesday, 08-Sep-2015 19:51:38 UTC\n"
        + "ParentServerConfigGeneration: 1\n"
        + "ParentServerMPMGeneration: 0\n"
        + "ServerUptimeSeconds: \n"
        + "ServerUptime: 1 hour 15 minutes 29 seconds\n"
        + "Load1: 0.04\n"
        + "Load5: 0.03\n"
        + "Load15: 0.05\n"
        + "Total Accesses: " + TOTAL_ACCESSES + "\n"
        + "Total kBytes: " + KBYTES + "\n"
        + "CPUUser: .36\n"
        + "CPUSystem: .26\n"
        + "CPUChildrenUser: 0\n"
        + "CPUChildrenSystem: 0\n"
        + "CPULoad: .044688\n"
        + "Uptime: " + UPTIME + "\n"
        + "ReqPerSec: " + RPS + "\n"
        + "BytesPerSec: " + BPS + "\n"
        + "BytesPerReq: " + BPR + "\n"
        + "BusyWorkers: " + BUSY_WORKERS + "\n"
        + "IdleWorkers: " + IDLE_WORKERS + "\n"
        + "Scoreboard: __________________K___W_K_____K______________K____"
        + repeat('.', OPEN_SLOTS)
        + "\n";
    ApacheStatusPoller.StatusFetcher fetcher = () -> new ByteArrayInputStream(statusText.getBytes("UTF-8"));

    ApacheStatusPoller poller = new ApacheStatusPoller(fetcher);

    List<Metric> metrics = poller.pollImpl(TIMESTAMP);

    Metric accesses = counter("Total_Accesses", TOTAL_ACCESSES);
    Metric kBytes = counter("Total_kBytes", KBYTES);
    Metric uptime = counter("Uptime", UPTIME);

    List<Metric> counters = UnmodifiableList.of(accesses, kBytes, uptime);
    Metric busyWorkers = gauge("BusyWorkers", BUSY_WORKERS);
    Metric idleWorkers = gauge("IdleWorkers", IDLE_WORKERS);
    List<Metric> gauges = UnmodifiableList.of(busyWorkers, idleWorkers);

    Metric waitingForConnection = scoreboard("WaitingForConnection", 45.0);
    Metric startingUp = scoreboard("StartingUp", 0.0);
    Metric readingRequest = scoreboard("ReadingRequest", 0.0);
    Metric sendingReply = scoreboard("SendingReply", 1.0);
    Metric keepalive = scoreboard("Keepalive", 4.0);
    Metric dnsLookup = scoreboard("DnsLookup", 0.0);
    Metric closingConnection = scoreboard("ClosingConnection", 0.0);
    Metric logging = scoreboard("Logging", 0.0);
    Metric gracefullyFinishing = scoreboard("GracefullyFinishing", 0.0);
    Metric idleCleanupOfWorker = scoreboard("IdleCleanupOfWorker", 0.0);
    Metric unknownState = scoreboard("UnknownState", 0.0);
    List<Metric> scoreboard = UnmodifiableList.of(waitingForConnection,
        startingUp, readingRequest, sendingReply, keepalive, dnsLookup, closingConnection,
        logging, gracefullyFinishing, idleCleanupOfWorker, unknownState);

    List<Metric> expected = new ArrayList<>();
    expected.addAll(counters);
    expected.addAll(gauges);
    expected.addAll(scoreboard);

    assertEquals(metrics, expected);
  }
}

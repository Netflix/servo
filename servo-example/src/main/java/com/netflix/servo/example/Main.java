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
package com.netflix.servo.example;

import com.netflix.servo.publish.AsyncMetricObserver;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.FileMetricObserver;
import com.netflix.servo.publish.JvmMetricPoller;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import com.netflix.servo.publish.atlas.AtlasMetricObserver;
import com.netflix.servo.publish.atlas.ServoAtlasConfig;
import com.netflix.servo.publish.graphite.GraphiteMetricObserver;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Main {


  private static final String CLUSTER = "nf.cluster";
  private static final String NODE = "nf.node";
  private static final String UNKNOWN = "unknown";

  private Main() {
  }

  private static MetricObserver rateTransform(MetricObserver observer) {
    final long heartbeat = 2 * Config.getPollInterval();
    return new CounterToRateMetricTransform(observer, heartbeat, TimeUnit.SECONDS);
  }

  private static MetricObserver async(String name, MetricObserver observer) {
    final long expireTime = 2000 * Config.getPollInterval();
    final int queueSize = 10;
    return new AsyncMetricObserver(name, observer, queueSize, expireTime);
  }

  private static MetricObserver createFileObserver() {
    final File dir = Config.getFileObserverDirectory();
    return rateTransform(new FileMetricObserver("servo-example", dir));
  }

  private static MetricObserver createGraphiteObserver() {
    final String prefix = Config.getGraphiteObserverPrefix();
    final String addr = Config.getGraphiteObserverAddress();
    return rateTransform(async("graphite", new GraphiteMetricObserver(prefix, addr)));
  }

  private static TagList getCommonTags() {
    final Map<String, String> tags = new HashMap<>();
    final String cluster = System.getenv("NETFLIX_CLUSTER");
    tags.put(CLUSTER, (cluster == null) ? UNKNOWN : cluster);
    try {
      tags.put(NODE, InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      tags.put(NODE, UNKNOWN);
    }
    return BasicTagList.copyOf(tags);
  }

  private static MetricObserver createAtlasObserver() {
    final ServoAtlasConfig cfg = Config.getAtlasConfig();
    final TagList common = getCommonTags();
    return rateTransform(async("atlas", new AtlasMetricObserver(cfg, common)));
  }

  private static void schedule(MetricPoller poller, List<MetricObserver> observers) {
    final PollRunnable task = new PollRunnable(poller, BasicMetricFilter.MATCH_ALL,
        true, observers);
    PollScheduler.getInstance().addPoller(task, Config.getPollInterval(), TimeUnit.SECONDS);
  }

  private static void initMetricsPublishing() throws Exception {
    final List<MetricObserver> observers = new ArrayList<>();
    if (Config.isFileObserverEnabled()) {
      observers.add(createFileObserver());
    }

    if (Config.isAtlasObserverEnabled()) {
      observers.add(createAtlasObserver());
    }

    if (Config.isGraphiteObserverEnabled()) {
      observers.add(createGraphiteObserver());
    }

    PollScheduler.getInstance().start();
    schedule(new MonitorRegistryMetricPoller(), observers);

    if (Config.isJvmPollerEnabled()) {
      schedule(new JvmMetricPoller(), observers);
    }
  }

  private static void initHttpServer() throws Exception {
    // Setup default endpoints
    final HttpServer server = HttpServer.create();
    server.createContext("/echo", new EchoHandler());

    // Hook to allow for graceful exit
    final Closeable c = () -> {
      PollScheduler.getInstance().stop();
      server.stop(5);
    };
    server.createContext("/exit", new ExitHandler(c));

    // Bind and start server
    server.bind(new InetSocketAddress(Config.getPort()), 0);
    server.start();
  }

  public static void main(String[] args) throws Exception {
    initMetricsPublishing();
    initHttpServer();
  }
}

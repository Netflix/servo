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
package com.netflix.servo.example;

import com.netflix.servo.publish.AsyncMetricObserver;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.FileMetricObserver;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;

import com.netflix.servo.publish.graphite.GraphiteMetricObserver;

import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.TimeUnit;

public class Main {

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

    private static void initMetricsPublishing() throws Exception {
        final List<MetricObserver> observers = new ArrayList<MetricObserver>();
        if (Config.isFileObserverEnabled()) {
            observers.add(createFileObserver());
        }

        if (Config.isGraphiteObserverEnabled()) {
            observers.add(createGraphiteObserver());
        }

        final MetricPoller poller = new MonitorRegistryMetricPoller();
        final PollRunnable task = new PollRunnable(poller, BasicMetricFilter.MATCH_ALL, observers);

        PollScheduler.getInstance().start();
        PollScheduler.getInstance().addPoller(task, Config.getPollInterval(), TimeUnit.SECONDS);
    }

    private static void initHttpServer() throws Exception {
        // Setup default endpoints
        final HttpServer server = HttpServer.create();
        server.createContext("/echo", new EchoHandler());

        // Hook to allow for graceful exit
        final Closeable c = new Closeable() {
            public void close() throws IOException {
                PollScheduler.getInstance().stop();
                server.stop(5);
            }
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

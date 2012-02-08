/*
 * #%L
 * servo-core
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.examples;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.CountingInputStream;
import com.google.common.io.CountingOutputStream;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.FileMetricObserver;
import com.netflix.servo.publish.JmxMetricPoller;
import com.netflix.servo.publish.LocalJmxConnector;
import com.netflix.servo.publish.MetricFilter;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import com.netflix.servo.publish.PrefixMetricFilter;
import com.netflix.servo.publish.RegexMetricFilter;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.Counters;
import com.netflix.servo.util.TaggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * An really basic echo server that uses the utility methods from
 * {@link com.netflix.servo.util.Counters}.
 */
public class EchoServerExample {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(EchoServerExample.class);

    private final int port;

    public EchoServerExample(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        AcceptTask task = new AcceptTask(port);
        Thread t = new Thread(task, "AcceptTask");
        t.start();
    }

    public void shutdown() {
        // Just an example, a real server should have a way to cleanly
        // shutdown
    }

    public static class AcceptTask implements Runnable {
        private static final String[] COUNTRIES = {"US", "CA", "GB", "IE"};
        private final ServerSocket ss;
        private final Random r = new Random();

        public AcceptTask(int port) throws IOException {
            ss = new ServerSocket(port);
        }

        public TagList getTags(Socket s) {
            String country = COUNTRIES[r.nextInt(COUNTRIES.length)];
            return BasicTagList.copyOf(new BasicTag("Country", country));
        }

        public void run() {
            while (true) {
                try {
                    Socket s = ss.accept();
                    TagList tags = getTags(s);
                    LOGGER.info("received connection from {} with tags {}",
                        s.getRemoteSocketAddress(), tags);

                    Counters.increment("RequestCount", tags);
                    ClientTask task = new ClientTask(tags, s);
                    Thread t = new Thread(task, "ClientTask");
                    t.start();
                } catch (IOException e) {
                    LOGGER.error("failure accepting connection", e);
                }
            }
        }
    }

    public static class ClientTask implements Runnable {
        private final TagList tags;
        private final Socket s;

        public ClientTask(TagList tags, Socket s) {
            this.tags = tags;
            this.s = s;
        }

        private void doWork() throws IOException {
            CountingInputStream input = null;
            CountingOutputStream output = null;
            try {
                input = new CountingInputStream(s.getInputStream());
                output = new CountingOutputStream(s.getOutputStream());
                ByteStreams.copy(input, output);
                Counters.increment("BytesIn", input.getCount());
                Counters.increment("BytesOut", output.getCount());
            } finally {
                Closeables.closeQuietly(input);
                Closeables.closeQuietly(output);
            }
        }

        public void run() {
            // Setup context so all counters increments in this thread will get
            // tagged
            TaggingContext.setTags(tags);

            try {
                doWork();
            } catch (IOException e) {
                SocketAddress a = s.getRemoteSocketAddress();
                LOGGER.error("failure handling connection from " + a, e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Schedule collection of monitor registry metrics every 10 seconds
        PollScheduler scheduler = PollScheduler.getInstance();
        scheduler.start();
        MetricObserver transform = new CounterToRateMetricTransform(
            new FileMetricObserver("serverstat", new File(".")),
            20, TimeUnit.SECONDS);
        PollRunnable task = new PollRunnable(
            new MonitorRegistryMetricPoller(),
            BasicMetricFilter.MATCH_ALL,
            transform);
        scheduler.addPoller(task, 10, TimeUnit.SECONDS);

        // Run server
        int port = 54321;
        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        EchoServerExample example = new EchoServerExample(port);
        example.start();
    }
}

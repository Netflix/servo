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
package com.netflix.servo.examples;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.google.common.io.CountingInputStream;
import com.google.common.io.CountingOutputStream;
import com.netflix.servo.monitor.DynamicCounter;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.FileMetricObserver;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * An really basic echo server that uses the utility methods from
 * {@link com.netflix.servo.monitor.DynamicCounter}
 */
public class EchoServerExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(EchoServerExample.class);
    public static final int DEFAULT_PORT = 54321;

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

        public AcceptTask(int port) throws IOException {
            ss = new ServerSocket(port);
        }

        public TagList getTags(Socket s) {
            // Find the remote country by using this socket
            int length = COUNTRIES.length;
            // force countryCode to be non-negative
            int countryCode = (s.getInetAddress().hashCode() % length + length) % length;
            String country = COUNTRIES[countryCode];
            return BasicTagList.of("Country", country);
        }

        public void run() {
            while (true) {
                try {
                    Socket s = ss.accept();
                    TagList tags = getTags(s);
                    LOGGER.info("received connection from {} with tags {}",
                        s.getRemoteSocketAddress(), tags);

                    DynamicCounter.increment("RequestCount", tags);
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
            Closer closer = Closer.create();
            CountingInputStream input = null;
            CountingOutputStream output = null;
            try {
                input = closer.register(new CountingInputStream(s.getInputStream()));
                output = closer.register(new CountingOutputStream(s.getOutputStream()));
                ByteStreams.copy(input, output);
                DynamicCounter.increment("BytesIn", tags, input.getCount());
                DynamicCounter.increment("BytesOut", tags, output.getCount());
            } catch (Throwable t) {
                throw closer.rethrow(t);
            } finally {
                closer.close();
            }
        }

        public void run() {
            // Setup context so all counters increments in this thread will get
            // tagged
            //TaggingContext.setTags(tags);

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

        final int heartbeatInterval = 20;
        MetricObserver transform = new CounterToRateMetricTransform(
            new FileMetricObserver("serverstat", new File(".")),
            heartbeatInterval, TimeUnit.SECONDS);

        PollRunnable task = new PollRunnable(
            new MonitorRegistryMetricPoller(),
            BasicMetricFilter.MATCH_ALL,
            transform);

        final int samplingInterval = 10;
        scheduler.addPoller(task, samplingInterval, TimeUnit.SECONDS);

        // Run server
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        EchoServerExample example = new EchoServerExample(port);
        example.start();
    }
}

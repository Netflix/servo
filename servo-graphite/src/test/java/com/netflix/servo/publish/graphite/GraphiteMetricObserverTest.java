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
package com.netflix.servo.publish.graphite;


import com.netflix.servo.Metric;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class GraphiteMetricObserverTest {
  private String getLocalHostIp() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostAddress();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadAddress1() throws Exception {
    new GraphiteMetricObserver("serverA", getLocalHostIp());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadAddress2() throws Exception {
    new GraphiteMetricObserver("serverA", "http://google.com");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadAddress3() throws Exception {
    new GraphiteMetricObserver("serverA", "socket://" + getLocalHostIp() + ":808");
  }
  
  private int getAvailablePort() {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSuccessfulSend() throws Exception {
    final int port = getAvailablePort();
    SocketReceiverTester receiver = new SocketReceiverTester(port);
    receiver.start();

    String host = getLocalHostIp() + ":" + port;
    GraphiteMetricObserver gw = new GraphiteMetricObserver("serverA", host);

    try {
      List<Metric> metrics = new ArrayList<>();
      metrics.add(BasicGraphiteNamingConventionTest.getOSMetric("AvailableProcessors"));

      gw.update(metrics);

      receiver.waitForConnected();

      String[] lines = receiver.waitForLines(1);
      assertEquals(1, lines.length);

      int found = lines[0].indexOf("serverA.java.lang.OperatingSystem.AvailableProcessors");
      assertEquals(found, 0);

    } finally {
      receiver.stop();
      gw.stop();
    }
  }

  @Test
  public void testReconnection() throws Exception {
    final int port = getAvailablePort();
    SocketReceiverTester receiver = new SocketReceiverTester(port);
    receiver.start();

    String host = getLocalHostIp() + ":" + port;
    GraphiteMetricObserver gw = new GraphiteMetricObserver("serverA", host);

    try {
      List<Metric> metrics = new ArrayList<>();
      metrics.add(BasicGraphiteNamingConventionTest.getOSMetric("AvailableProcessors"));

      gw.update(metrics);

      receiver.waitForConnected();

      String[] lines = receiver.waitForLines(1);
      assertEquals(1, lines.length);

      int found = lines[0].indexOf("serverA.java.lang.OperatingSystem.AvailableProcessors");
      assertEquals(found, 0);

      // restarting the receiver
      receiver.stop();
      receiver = new SocketReceiverTester(port);
      receiver.start();

      // the first write does not trigger exception given how TCP works
      gw.update(metrics);
      assertEquals(0, gw.getFailedUpdateCount());

      // the second update will fail and thus closes the connection
      gw.update(metrics);
      assertEquals(1, gw.getFailedUpdateCount());

      // the third update will establish a new connection
      gw.update(metrics);
      assertEquals(1, gw.getFailedUpdateCount());
      receiver.waitForConnected();
      receiver.waitForLines(1);
      found = lines[0].indexOf("serverA.java.lang.OperatingSystem.AvailableProcessors");
      assertEquals(found, 0);
    } finally {
      receiver.stop();
      gw.stop();
    }
  }
}

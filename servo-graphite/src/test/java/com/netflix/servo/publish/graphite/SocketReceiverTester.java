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
package com.netflix.servo.publish.graphite;

import javax.net.ServerSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class SocketReceiverTester implements Runnable {
    private final ServerSocket acceptor;
    private Socket s;

    private String[] lines = new String[100];
    private volatile boolean running = true;
    private volatile boolean connected = false;
    private volatile int linesRead = 0;
    private volatile int linesWritten = 0;

    public SocketReceiverTester(int port) throws IOException {
        ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();
        acceptor = socketFactory.createServerSocket();
        acceptor.setReuseAddress(true);
        acceptor.bind(new InetSocketAddress(port));
    }

    @Override
    public void run() {
        while (running) {
            try {
                s = acceptor.accept();
                synchronized (this) {
                    connected = true;
                    notify();
                }
                BufferedReader stream = new BufferedReader(
                    new InputStreamReader(s.getInputStream(), "UTF-8"));

                while (running) {
                    String line = stream.readLine();
                    synchronized (this) {
                        lines[linesWritten++ % lines.length] = line;
                        notify();
                    }
                }
            } catch (IOException e) {
                synchronized (this) {
                    connected = false;
                    linesWritten = 0;
                    linesRead = 0;
                    notify();
                }
            }
        }
    }

    private Thread thread;

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void stop() throws Exception {
        running = false;
        if (s != null) {
            s.close();
        }
        acceptor.close();
        thread.interrupt();
        thread.join();
    }

    public String[] waitForLines(int waitingFor) throws Exception {
        long start = System.currentTimeMillis();
        synchronized (this) {
            while (linesWritten < linesRead + waitingFor) {
                if (!connected) {
                    throw new IllegalArgumentException("Not connected!");
                }
                if (System.currentTimeMillis() - start > 1000) {
                    throw new InterruptedException("Timed out!");
                }
                wait(100);
            }
            return Arrays.copyOfRange(lines, linesRead, linesRead + waitingFor);
        }
    }

    public void waitForConnected() throws Exception {
        long start = System.currentTimeMillis();
        synchronized (this) {
            while (!connected) {
                if (System.currentTimeMillis() - start > 1000) {
                    throw new InterruptedException("Timed out!");
                }
                wait(100);
            }
        }

    }
}

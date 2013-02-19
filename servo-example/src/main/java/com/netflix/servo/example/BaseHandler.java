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


import com.google.common.io.CountingInputStream;
import com.google.common.io.CountingOutputStream;

import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public abstract class BaseHandler implements HttpHandler {

    private final Timer latency = Monitors.newTimer("latency");

    private final Counter bytesReceived = Monitors.newCounter("bytesReceived");
    private final Counter bytesSent = Monitors.newCounter("bytesSent");

    public void init() {
        Monitors.registerObject(this);
    }

    public void handle(HttpExchange exchange) throws IOException {
        CountingInputStream input = new CountingInputStream(exchange.getRequestBody());
        CountingOutputStream output = new CountingOutputStream(exchange.getResponseBody());
        exchange.setStreams(input, output);
        Stopwatch stopwatch = latency.start();
        try {
            handleImpl(exchange);
        } finally {
            stopwatch.stop();
            bytesReceived.increment(input.getCount());
            bytesSent.increment(output.getCount());
        }
    }

    protected abstract void handleImpl(HttpExchange exchange) throws IOException;
}

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

import com.sun.net.httpserver.HttpExchange;

import java.io.Closeable;
import java.io.IOException;

public class ExitHandler extends BaseHandler {

  private final Closeable server;

  public ExitHandler(Closeable server) {
    super();
    this.server = server;
    init();
  }

  @Override
  protected void handleImpl(HttpExchange exchange) throws IOException {
    try {
      exchange.sendResponseHeaders(200, 0);
      exchange.close();
    } finally {
      server.close();
    }
  }
}

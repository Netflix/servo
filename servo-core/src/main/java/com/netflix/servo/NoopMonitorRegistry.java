/*
 * Copyright 2020 Netflix, Inc.
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
package com.netflix.servo;

import com.netflix.servo.monitor.Monitor;

import java.util.Collection;
import java.util.Collections;

/**
 * Monitor registry implementation that does as little as possible.
 */
public class NoopMonitorRegistry implements MonitorRegistry {

  public NoopMonitorRegistry() {
  }

  @Override
  public Collection<Monitor<?>> getRegisteredMonitors() {
    return Collections.emptyList();
  }

  @Override
  public void register(Monitor<?> monitor) {
  }

  @Override
  public void unregister(Monitor<?> monitor) {
  }

  @Override
  public boolean isRegistered(Monitor<?> monitor) {
    return false;
  }
}

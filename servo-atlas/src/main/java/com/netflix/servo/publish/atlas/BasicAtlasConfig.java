/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.servo.publish.atlas;

/**
 * A simple implementation of {@link ServoAtlasConfig} that uses system properties to get
 * values.
 */
public class BasicAtlasConfig implements ServoAtlasConfig {
  @Override
  public String getAtlasUri() {
    return System.getProperty("servo.atlas.uri");
  }

  @Override
  public int getPushQueueSize() {
    String pushQueueSize = System.getProperty("servo.atlas.queueSize", "1000");
    return Integer.parseInt(pushQueueSize);
  }

  @Override
  public boolean shouldSendMetrics() {
    String enabled = System.getProperty("servo.atlas.enabled", "true");
    return Boolean.parseBoolean(enabled);
  }

  @Override
  public int batchSize() {
    String batch = System.getProperty("servo.atlas.batchSize", "10000");
    return Integer.parseInt(batch);
  }
}

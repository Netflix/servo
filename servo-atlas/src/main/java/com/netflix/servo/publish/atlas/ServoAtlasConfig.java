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
 * Configuration for the servo to atlas interface.
 */
public interface ServoAtlasConfig {
  /**
   * Return the URI used to POST values to atlas.
   */
  String getAtlasUri();

  /**
   * Return the size of the queue to be used when pushing metrics to
   * the atlas backends. A value of 1000 is quite safe here, but might need
   * to be tweaked if attempting to send hundreds of batches per second.
   */
  int getPushQueueSize();


  /**
   * Whether we should send metrics to atlas. This can be used when running in a dev environment
   * for example to avoid affecting production metrics by dev machines.
   */
  boolean shouldSendMetrics();

  /**
   * The maximum size of the batch of metrics to be sent to atlas.
   * If attempting to send more metrics than this value,
   * the {@link AtlasMetricObserver} will split them into batches before sending
   * them to the atlas backends.
   * <p/>
   * A value of 10000 works well for most workloads.
   */
  int batchSize();
}

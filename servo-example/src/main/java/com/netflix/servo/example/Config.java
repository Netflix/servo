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

import com.netflix.servo.monitor.Pollers;
import com.netflix.servo.publish.atlas.ServoAtlasConfig;

import java.io.File;

/**
 * Utility class dealing with different settings used to run the examples.
 */
public final class Config {
  private Config() {
  }

  /**
   * Port number for the http server to listen on.
   */
  public static int getPort() {
    return Integer.parseInt(System.getProperty("servo.example.port", "12345"));
  }

  /**
   * How frequently to poll metrics in seconds and report to observers.
   */
  public static long getPollInterval() {
    return Pollers.getPollingIntervals().get(0) / 1000L;
  }

  /**
   * Should we report metrics to the file observer? Default is true.
   */
  public static boolean isFileObserverEnabled() {
    return Boolean.valueOf(System.getProperty("servo.example.isFileObserverEnabled", "true"));
  }

  /**
   * Default directory for writing metrics files. Default is /tmp.
   */
  public static File getFileObserverDirectory() {
    return new File(System.getProperty("servo.example.fileObserverDirectory", "/tmp"));
  }

  /**
   * Should we report metrics to graphite? Default is false.
   */
  public static boolean isGraphiteObserverEnabled() {
    return Boolean.valueOf(System.getProperty("servo.example.isGraphiteObserverEnabled",
        "false"));
  }

  /**
   * Prefix to use when reporting data to graphite. Default is servo.
   */
  public static String getGraphiteObserverPrefix() {
    return System.getProperty("servo.example.graphiteObserverPrefix", "servo");
  }

  /**
   * Address for reporting to graphite. Default is localhost:2003.
   */
  public static String getGraphiteObserverAddress() {
    return System.getProperty("servo.example.graphiteObserverAddress", "localhost:2003");
  }

  /**
   * Should we report metrics to atlas? Default is false.
   */
  public static boolean isAtlasObserverEnabled() {
    return Boolean.valueOf(System.getProperty("servo.example.isAtlasObserverEnabled", "false"));
  }

  /**
   * Prefix to use when reporting data to graphite. Default is servo.
   */
  public static String getAtlasObserverUri() {
    return System.getProperty("servo.example.atlasObserverUri",
        "http://localhost:7101/api/v1/publish");
  }

  /**
   * Should we poll the standard jvm mbeans? Default is true.
   */
  public static boolean isJvmPollerEnabled() {
    return Boolean.valueOf(System.getProperty("servo.example.isJvmPollerEnabled", "true"));
  }

  /**
   * Get config for the atlas observer.
   */
  public static ServoAtlasConfig getAtlasConfig() {
    return new ServoAtlasConfig() {
      @Override
      public String getAtlasUri() {
        return getAtlasObserverUri();
      }

      @Override
      public int getPushQueueSize() {
        return 1000;
      }

      @Override
      public boolean shouldSendMetrics() {
        return isAtlasObserverEnabled();
      }

      @Override
      public int batchSize() {
        return 10000;
      }
    };
  }
}

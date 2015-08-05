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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

import java.util.regex.Pattern;

@State(Scope.Thread)
public class ValidCharactersBench {
  private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_\\-\\.]");

  static String oldRegexMethod(String str) {
    return INVALID_CHARS.matcher(str).replaceAll("_");
  }

  @Threads(1)
  @Benchmark
  public void testUsingRegex(Blackhole bh) {
    String sourceStr = "netflix.streaming.vhs.server.pbstats.bitrate.playedSecs";
    bh.consume(oldRegexMethod(sourceStr));
  }

  @Threads(1)
  @Benchmark
  public void testNewByHand(Blackhole bh) {
    String sourceStr = "netflix.streaming.vhs.server.pbstats.bitrate.playedSecs";
    bh.consume(ValidCharacters.toValidCharset(sourceStr));
  }
}

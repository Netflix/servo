/**
 * Copyright 2014 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.servo.monitor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PollersTest {
  @Test
  public void testParseOneEntry() throws Exception {
    long[] expected1 = {1L};
    assertEquals(Pollers.parse("1"), expected1);

    long[] expected2 = {42000L};
    assertEquals(Pollers.parse("42000"), expected2);
  }

  @Test
  public void testParseInvalid() throws Exception {
    assertEquals(Pollers.parse("0"), Pollers.DEFAULT_PERIODS);
    assertEquals(Pollers.parse("-1"), Pollers.DEFAULT_PERIODS);
    assertEquals(Pollers.parse("1L"), Pollers.DEFAULT_PERIODS);
    assertEquals(Pollers.parse("100,-1"), Pollers.DEFAULT_PERIODS);
    assertEquals(Pollers.parse("100,0"), Pollers.DEFAULT_PERIODS);
  }

  @Test
  public void testParseMultiple() throws Exception {
    long[] expected = {60000L, 10000L, 2000L};
    assertEquals(Pollers.parse("60000,10000,2000"), expected);
  }

}

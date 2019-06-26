/**
* Copyright 2019 Netflix, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.netflix.servo.util;

import com.netflix.servo.util.ThreadCpuStats.CpuUsage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ThreadCpuStatsTest {

  @Test
  public void testToPercent() {
    Assert.assertEquals(0, ThreadCpuStats.toPercent(0L, 0L), 0.0);
    Assert.assertEquals(12.5, ThreadCpuStats.toPercent(25L, 200L), 0.0);
  }

  @Test
  public void testToDuration() {
    Assert.assertEquals("P6WT1H",
        ThreadCpuStats.toDuration(3_632_400_000_000_016L));
  }

  @Test
  public void testGetOverall()  {
    final CpuUsage cpuUsage = ThreadCpuStats.getInstance().getOverallCpuUsage();
    Assert.assertEquals(0L, cpuUsage.getOverall());
  }

  @Test
  public void testGetOneMinute()  {
    final CpuUsage cpuUsage = ThreadCpuStats.getInstance().getOverallCpuUsage();
    Assert.assertEquals(0L, cpuUsage.getOneMinute());
  }

  @Test
  public void testGetFiveMinute()  {
    final CpuUsage cpuUsage = ThreadCpuStats.getInstance().getOverallCpuUsage();
    Assert.assertEquals(0L, cpuUsage.getFiveMinute());
  }

  @Test
  public void testGetFifteenMinute()  {
    final CpuUsage cpuUsage = ThreadCpuStats.getInstance().getOverallCpuUsage();
    Assert.assertEquals(0L, cpuUsage.getFifteenMinute());
  }
}

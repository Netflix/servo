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
package com.netflix.servo.monitor;

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class BucketConfigTest {

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullTimeUnit() throws Exception {
    new BucketConfig.Builder().withTimeUnit(null).build();
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullBuckets() throws Exception {
    new BucketConfig.Builder().withBuckets(null).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyBuckets() throws Exception {
    new BucketConfig.Builder().withBuckets(new long[0]).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfOrderBuckets() throws Exception {
    new BucketConfig.Builder().withBuckets(new long[]{0, 2, 1}).build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDuplicateBuckets() throws Exception {
    new BucketConfig.Builder().withBuckets(new long[]{0, 1, 1, 2}).build();
  }

  @Test
  public void testAccessors() throws Exception {
    BucketConfig config = new BucketConfig.Builder()
        .withTimeUnit(TimeUnit.SECONDS)
        .withBuckets(new long[]{7, 8, 9})
        .build();

    assertEquals(config.getTimeUnit(), TimeUnit.SECONDS);
    assertEquals(config.getBuckets(), new long[]{7, 8, 9});
  }

  @Test
  public void testEquals() throws Exception {
    BucketConfig config1 = new BucketConfig.Builder()
        .withTimeUnit(TimeUnit.SECONDS)
        .withBuckets(new long[]{7, 8, 9})
        .build();

    BucketConfig config2 = new BucketConfig.Builder()
        .withTimeUnit(TimeUnit.SECONDS)
        .withBuckets(new long[]{7, 8, 9})
        .build();

    BucketConfig config3 = new BucketConfig.Builder()
        .withTimeUnit(TimeUnit.SECONDS)
        .withBuckets(new long[]{7, 8, 91})
        .build();

    assertNotNull(config1);
    assertFalse(config1.toString().equals(config3.toString()));
    assertTrue(config1.equals(config1));
    assertTrue(config1.equals(config2));
    assertFalse(config1.equals(config3));
  }

  @Test
  public void testHashCode() throws Exception {
    BucketConfig config1 = new BucketConfig.Builder()
        .withTimeUnit(TimeUnit.SECONDS)
        .withBuckets(new long[]{7, 8, 9})
        .build();

    BucketConfig config2 = new BucketConfig.Builder()
        .withTimeUnit(TimeUnit.SECONDS)
        .withBuckets(new long[]{7, 8, 9})
        .build();

    BucketConfig config3 = new BucketConfig.Builder()
        .withTimeUnit(TimeUnit.SECONDS)
        .withBuckets(new long[]{7, 8, 91})
        .build();

    assertTrue(config1.hashCode() == config1.hashCode());
    assertTrue(config1.hashCode() == config2.hashCode());
    assertTrue(config1.hashCode() != config3.hashCode());
  }
}

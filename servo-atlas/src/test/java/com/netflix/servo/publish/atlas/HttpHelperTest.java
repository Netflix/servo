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

import org.testng.annotations.Test;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

/**
 * Basic tests for {@link HttpHelper}.
 */
public class HttpHelperTest {
  @Test
  public void testSendAll() throws Exception {
    List<Observable<Integer>> batches = new ArrayList<>();
    int expectedSum = 0;
    for (int i = 1; i <= 5; ++i) {
      batches.add(Observable.just(i));
      expectedSum += i;
    }

    HttpHelper httpHelper = new HttpHelper(null);
    int sent = httpHelper.sendAll(batches, expectedSum, 100L);
    assertEquals(sent, expectedSum);

    // now add an observable that should timeout
    batches.add(Observable.<Integer>never());
    int partial = httpHelper.sendAll(batches, expectedSum, 100L);
    assertEquals(partial, expectedSum);
  }

  @Test
  public void testSendAllSlow() throws Exception {
    Observable<Integer> interval = Observable.interval(400,
        TimeUnit.MILLISECONDS).map(l -> l.intValue() + 1);

    // now add an observable that should timeout
    List<Observable<Integer>> batches = new ArrayList<>();
    batches.add(interval);

    int expectedSum = 3; // 1 + 2 should have been received from interval
    for (int i = 1; i <= 5; ++i) {
      batches.add(Observable.just(i));
      expectedSum += i;
    }

    HttpHelper httpHelper = new HttpHelper(null);
    int partial = httpHelper.sendAll(batches, expectedSum, 1000L);
    assertEquals(partial, expectedSum);
  }

}

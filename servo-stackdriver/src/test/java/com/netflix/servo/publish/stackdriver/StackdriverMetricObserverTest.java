/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.publish.stackdriver;

import com.netflix.servo.Metric;
import com.netflix.servo.tag.BasicTagList;
import com.stackdriver.api.custommetrics.CustomMetricsPoster;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * StackdriverMetricObserver tests
 */
public class StackdriverMetricObserverTest {
	private static final Random random = new Random();
	final CustomMetricsPoster client = new CustomMetricsPoster("<api_key>");
    final StackdriverMetricObserver observer = new StackdriverMetricObserver("testObserver", client);

    @Test(enabled=false, description="tests metrics posting to Stackdriver")
    public void testUpdate() throws Exception {
        List<Metric> metrics = new ArrayList<Metric>();

	    // integer
		metrics.add(new Metric("testInteger", BasicTagList.EMPTY, System.currentTimeMillis(), random.nextInt(100)+1));

	    // double
		metrics.add(new Metric("testDecimal", BasicTagList.EMPTY, System.currentTimeMillis(), random.nextInt(100)+1.1));

	    // longer double
		metrics.add(new Metric("testBiggerDecimal", BasicTagList.EMPTY, System.currentTimeMillis(), random.nextInt(100)+1.1135791357935979593571975));

	    // boolean
	    metrics.add(new Metric("testBoolean", BasicTagList.EMPTY, System.currentTimeMillis(), random.nextBoolean()));


		// publish the metrics
	    try {
            observer.update(metrics);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

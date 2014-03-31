/**
 * Copyright 2014 StudyBlue, Inc.
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
	    final long time = System.currentTimeMillis();

	    // integer
		metrics.add(new Metric("testInteger", BasicTagList.EMPTY, time, random.nextInt(100)+1));

	    // double
		metrics.add(new Metric("testDecimal", BasicTagList.EMPTY, time, random.nextInt(100)+1.1));

	    // longer double
		metrics.add(new Metric("testBiggerDecimal", BasicTagList.EMPTY, time, random.nextDouble()*100.0));

	    // boolean
	    metrics.add(new Metric("testBoolean", BasicTagList.EMPTY, time, random.nextBoolean()));

	    // string double
	    metrics.add(new Metric("testStringDouble", BasicTagList.EMPTY, time, ""+random.nextDouble()*100.0));

	    // string value (should be ignored)
	    metrics.add(new Metric("testString", BasicTagList.EMPTY, time, "test-"+time));


		// publish the metrics
	    try {
            observer.update(metrics);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

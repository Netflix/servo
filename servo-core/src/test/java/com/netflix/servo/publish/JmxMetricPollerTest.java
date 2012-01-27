/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.publish;

import com.google.common.collect.ImmutableMap;

import com.netflix.servo.BasicTagList;
import com.netflix.servo.TagList;

import java.util.Map;

import javax.management.ObjectName;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class JmxMetricPollerTest {

    @Test
    public void testPoll() throws Exception {
        JmxMetricPoller p = new JmxMetricPoller(
            new LocalJmxConnector(),
            new ObjectName("*:*"),
            new BasicMetricFilter(false));
        System.out.println(p.poll(new BasicMetricFilter(true)));
    }
}

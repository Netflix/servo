/**
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.servo.monitor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class PublishingPolicyTest extends AbstractMonitorTest<BasicCounter> {

    public BasicCounter newInstance(String name) {
        return new BasicCounter(MonitorConfig.builder(name).build());
    }

    @Test
    public void testDefaultPolicy() throws Exception {
        assertEquals(
            newInstance("A").getConfig().getPublishingPolicy(),
            DefaultPublishingPolicy.getInstance());
    }

    private static class OtherPolicy implements PublishingPolicy {
        static final OtherPolicy INSTANCE = new OtherPolicy();
    }

    @Test
    public void testEqualsPolicy() throws Exception {
        BasicCounter other = new BasicCounter(
            MonitorConfig.builder("name").withPublishingPolicy(OtherPolicy.INSTANCE).build());
        BasicCounter dflt = newInstance("name");

        assertNotEquals(other, dflt);
        assertNotEquals(other.hashCode(), dflt.hashCode());
    }
}

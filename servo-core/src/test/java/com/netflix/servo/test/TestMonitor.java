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
package com.netflix.servo.test;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorTags;
import com.netflix.servo.tag.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestMonitor {

    @Monitor(name="testCounter", type = DataSourceType.COUNTER,
            description = "Monitor for doing testing")
    public final AtomicInteger counter = new AtomicInteger(0);

    @MonitorTags
    public final List<Tag> tagList = new ArrayList<Tag>(10);

    public TestMonitor(){}

    public TestMonitor(Collection<Tag> tags){
        tagList.addAll(tags);
    }

    public void increment(){
        counter.incrementAndGet();
    }
}

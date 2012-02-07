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
package com.netflix.servo.examples;

import com.netflix.servo.BasicTagList;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.InjectableTag;
import com.netflix.servo.Tag;
import com.netflix.servo.TagList;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorId;
import com.netflix.servo.annotations.MonitorTags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple Sample Code for getting a monitor into JMX.
 */
public class BasicExample {

    @Monitor(name = "SampleCounter", type = DataSourceType.COUNTER,
            description = "Sample counting monitor",
            tags = {"sample=simple"})
    public final AtomicInteger counter = new AtomicInteger(0);

    @Monitor(name = "SampleGauge", type = DataSourceType.GAUGE,
            description = "Sample gauge monitor",
            tags = {"sample=simple"})
    private long sampleGuage = 0;

    @MonitorId
    private final String id;

    @MonitorTags
    public final TagList tags;

    public BasicExample() {
        this(null, BasicTagList.EMPTY);
    }

    public BasicExample(String id, Iterable<Tag> tags) {
        this.id = id;
        this.tags = new BasicTagList(tags);
    }


    public synchronized void setSampleGauge(long val){
        sampleGuage = val;
    }

    public synchronized long getSampleGauge(){
        return sampleGuage;
    }

    public static void main(String[] args) throws InterruptedException {
        List<Tag> tags = new ArrayList<Tag>(2);
        tags.add(InjectableTag.HOSTNAME);
        tags.add(InjectableTag.IP);

        String id = null;
        if (args.length > 0) {
            id = args[0];
        }
        BasicExample example = new BasicExample(id, tags);

        DefaultMonitorRegistry.getInstance().registerObject(example);

        while(true) {
            example.counter.incrementAndGet();
            example.setSampleGauge(Math.round(Math.random() * 1000));
            Thread.sleep(10000);
        }
    }
}

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

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorContext;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorTags;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.tag.InjectableTag;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple Sample Code for getting a monitor into JMX.
 */
public class BasicExample {

    @Monitor(name = "SampleCounter", type = DataSourceType.COUNTER,
            description = "Sample counting monitor")
    public final AtomicInteger counter = new AtomicInteger(0);

    @Monitor(name = "SampleGauge", type = DataSourceType.GAUGE,
            description = "Sample gauge monitor")
    private long sampleGuage = 0;

    @MonitorTags
    public final TagList tags;

    public BasicExample() {
        this.tags = SortedTagList.EMPTY;
    }

    public BasicExample(Collection<Tag> tags) {
        this.tags = SortedTagList.builder().withTags(tags).build();
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

        Counter counter = new BasicCounter(new MonitorContext.Builder("test1").withTags(tags).build());


        String id = null;
        if (args.length > 0) {
            id = args[0];
        }
        BasicExample example = new BasicExample(tags);

        DefaultMonitorRegistry.getInstance().registerAnnotatedObject(example);
        DefaultMonitorRegistry.getInstance().register(counter);


        while(true) {
            example.counter.incrementAndGet();
            counter.increment();
            example.setSampleGauge(Math.round(Math.random() * 1000));
            Thread.sleep(10000);
        }
    }
}

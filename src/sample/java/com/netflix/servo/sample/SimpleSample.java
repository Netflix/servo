/*
 * Copyright (c) 2012. Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.netflix.servo.sample;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.InjectableTag;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.Tag;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorTags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: gorzell
 * Date: 1/6/12
 * Time: 12:45 PM
 */
public class SimpleSample {

    @Monitor(name = "SampleCounter", type = DataSourceType.COUNTER,
            description = "Sample counting monitor", tags = {
            "sample=simple"})
    public final AtomicInteger counter = new AtomicInteger(0);

    @Monitor(name = "SampleGauge", type = DataSourceType.GAUGE,
            description = "Sample gauge monitor", tags = {
            "sample=simple"})
    private long sampleGuage = 0;

    @MonitorTags
    public final List<Tag> tagList = new ArrayList<Tag>(10);

    public SimpleSample() {
    }

    public SimpleSample(Collection<Tag> tags) {
        tagList.addAll(tags);
    }
    
    public synchronized void setSampleGauage(long val){
        sampleGuage = val;
    }
    
    public synchronized long getSampleGuage(){
        return sampleGuage;
    }

    public static void main(String[] args) throws InterruptedException {
        List<Tag> tags = new ArrayList<Tag>(2);
        tags.add(InjectableTag.HOSTNAME);
        tags.add(InjectableTag.IP);
        
        SimpleSample sample = new SimpleSample(tags);

        DefaultMonitorRegistry.getInstance().registerObject(sample);
        
        while(true){
            sample.counter.incrementAndGet();
            sample.setSampleGauage(Math.round(Math.random()*1000));
            Thread.sleep(60000);
        }
    }
}

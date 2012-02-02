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

import com.amazonaws.auth.PropertiesCredentials;
import com.netflix.servo.*;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.annotations.MonitorTags;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollCallable;
import com.netflix.servo.publish.cloudwatch.CloudWatchMetricObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sample of publishing the SimpleSample to CloudWatch
 */
public class CloudWatchSample {
    
    public static void main(String[] args) throws Exception {
        if(args.length != 1){
            System.err.println("Please specify the path to a properties file with your amazon keys.");
            return;
        }

        CloudWatchMetricObserver observer = new CloudWatchMetricObserver("SampleObserver", "SampleDomain",
                new PropertiesCredentials(new File(args[0])));

        List<Tag> tags = new ArrayList<Tag>(2);
        tags.add(InjectableTag.HOSTNAME);
        tags.add(InjectableTag.IP);

        SimpleSample sample = new SimpleSample(tags);

        DefaultMonitorRegistry.getInstance().registerObject(sample);

        PollCallable poller = new PollCallable(new MonitorRegistryMetricPoller(), BasicMetricFilter.MATCH_ALL);


        while(true){
            sample.counter.incrementAndGet();
            sample.setSampleGauage(Math.round(Math.random() * 1000));
            observer.update(poller.call());
            Thread.sleep(60000);
        }
    }


public static class SimpleSample {

    @Monitor(name = "SampleCounter", type = DataSourceType.COUNTER,
            description = "Sample counting monitor", tags = {
            "sample=counter"})
    public final AtomicInteger counter = new AtomicInteger(0);

    @Monitor(name = "SampleGauge", type = DataSourceType.GAUGE,
            description = "Sample gauge monitor", tags = {
            "sample=gauge"})
    private long sampleGuage = 0;

    @MonitorTags
    public TagList tagList = BasicTagList.EMPTY;

    public SimpleSample() {
    }

    public SimpleSample(Collection<Tag> tags) {
        tagList = new BasicTagList(tags);
    }

    public synchronized void setSampleGauage(long val){
        sampleGuage = val;
    }

    public synchronized long getSampleGuage(){
        return sampleGuage;
    }
}
}
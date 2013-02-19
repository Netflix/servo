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
package com.netflix.servo.examples;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.netflix.servo.*;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollCallable;
import com.netflix.servo.publish.cloudwatch.CloudWatchMetricObserver;
import com.netflix.servo.tag.InjectableTag;
import com.netflix.servo.tag.Tag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample of publishing the SimpleSample to CloudWatch
 */
public class CloudWatchExample {

    public static void main(String[] args) throws Exception {
        if(args.length != 1){
            System.err.println("Please specify the path to a properties file with your amazon keys.");
            return;
        }

        CloudWatchMetricObserver observer = new CloudWatchMetricObserver("SampleObserver", "SampleDomain",
                new ClasspathPropertiesFileCredentialsProvider());

        List<Tag> tags = new ArrayList<Tag>(2);
        tags.add(InjectableTag.HOSTNAME);
        tags.add(InjectableTag.IP);

        BasicExample example = new BasicExample(tags);

        DefaultMonitorRegistry.getInstance().register(Monitors.newObjectMonitor(example));

        PollCallable poller = new PollCallable(new MonitorRegistryMetricPoller(), BasicMetricFilter.MATCH_ALL);


        while(true){
            example.setSampleGauge(Math.round(Math.random() * 1000));
            observer.update(poller.call());
            Thread.sleep(60000);
        }
    }
}

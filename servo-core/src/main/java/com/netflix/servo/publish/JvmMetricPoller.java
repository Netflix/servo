/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
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

import static com.netflix.servo.annotations.DataSourceType.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.netflix.servo.BasicTag;
import com.netflix.servo.BasicTagList;
import com.netflix.servo.StandardTagKeys;
import com.netflix.servo.Tag;
import com.netflix.servo.TagList;
import com.netflix.servo.annotations.DataSourceType;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JvmMetricPoller implements MetricPoller {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(JvmMetricPoller.class);

    private static final String LOADED_COUNT = "LoadedClassCount";
    private static final String TOTAL_LOADED_COUNT = "TotalLoadedClassCount";
    private static final String UNLOADED_COUNT = "UnloadedClassCount";

    private static final String TOTAL_COMPILATION_TIME = "TotalCompilationTime";

    private static final String COLLECTOR_NAME = "CollectorName";
    private static final String COLLECTION_COUNT = "CollectionCount";
    private static final String COLLECTION_TIME = "CollectionTime";

    private static final String FINALIZATION_PENDING_COUNT =
        "ObjectPendingFinalizationCount";

    private static final String AVAILABLE_PROCESSORS = "AvailableProcessors";
    private static final String SYSTEM_LOAD_AVERAGE = "SystemLoadAverage";

    private static final String THREAD_COUNT = "ThreadCount";
    private static final String DAEMON_THREAD_COUNT = "DaemonThreadCount";
    private static final String PEAK_THREAD_COUNT = "PeakThreadCount";

    TagList dfltTags(Class<?> c, DataSourceType type) {
        String key = StandardTagKeys.CLASS_NAME.getKeyName();
        String value = c.getCanonicalName();
        Tag classTag = new BasicTag(key, value);
        return new BasicTagList(ImmutableList.of(classTag, type));
    }

    void getClassLoadingMetrics(
            List<Metric> metrics, ClassLoadingMXBean mbean) {
        if (mbean != null) {
            TagList tags = dfltTags(ClassLoadingMXBean.class, GAUGE);
            long now = System.currentTimeMillis();
            metrics.add(new Metric(
                LOADED_COUNT, 
                tags, now,
                mbean.getLoadedClassCount()));
            metrics.add(new Metric(
                TOTAL_LOADED_COUNT,
                tags, now,
                mbean.getTotalLoadedClassCount()));
            metrics.add(new Metric(
                UNLOADED_COUNT,
                tags, now,
                mbean.getUnloadedClassCount()));
        } else {
            LOGGER.debug("ClassLoadingMXBean is null");
        }
    }

    void getCompilationMetrics(List<Metric> metrics, CompilationMXBean mbean) {
        if (mbean != null) {
            TagList tags = dfltTags(CompilationMXBean.class, COUNTER);
            long now = System.currentTimeMillis();
            metrics.add(new Metric(
                TOTAL_COMPILATION_TIME, 
                tags, now,
                mbean.getTotalCompilationTime()));
        } else {
            LOGGER.debug("CompilationMXBean is null");
        }
    }

    void getGarbageCollectorMetrics(
            List<Metric> metrics, GarbageCollectorMXBean mbean) {
        // TODO: collector name
        TagList tags = dfltTags(GarbageCollectorMXBean.class, COUNTER);
        long now = System.currentTimeMillis();
        metrics.add(new Metric(
            COLLECTION_COUNT, 
            tags, now,
            mbean.getCollectionCount()));
        metrics.add(new Metric(
            COLLECTION_TIME, 
            tags, now,
            mbean.getCollectionTime()));
    }

    void getMemoryMetrics(List<Metric> metrics, MemoryMXBean mbean) {
        TagList tags = dfltTags(MemoryMXBean.class, GAUGE);
        long now = System.currentTimeMillis();
        metrics.add(new Metric(
            FINALIZATION_PENDING_COUNT, 
            tags, now,
            mbean.getObjectPendingFinalizationCount()));
        // TODO heap/non_heap
    }

    void getMemoryPoolMetrics(List<Metric> metrics, MemoryPoolMXBean mbean) {
        // TODO: collector name
        TagList tags = dfltTags(MemoryPoolMXBean.class, GAUGE);
        long now = System.currentTimeMillis();
        /*metrics.add(new Metric(
            FINALIZATION_PENDING_COUNT, 
            tags, now,
            mbean.getObjectPendingFinalizationCount()));*/
    }

    void getOperatingSystemMetrics(
            List<Metric> metrics, OperatingSystemMXBean mbean) {
        TagList tags = dfltTags(OperatingSystemMXBean.class, GAUGE);
        long now = System.currentTimeMillis();
        metrics.add(new Metric(
            AVAILABLE_PROCESSORS, 
            tags, now,
            mbean.getAvailableProcessors()));
        metrics.add(new Metric(
            SYSTEM_LOAD_AVERAGE, 
            tags, now,
            mbean.getSystemLoadAverage()));
    }

    void getThreadMetrics(List<Metric> metrics, ThreadMXBean mbean) {
        TagList tags = dfltTags(ThreadMXBean.class, GAUGE);
        long now = System.currentTimeMillis();
        metrics.add(new Metric(
            THREAD_COUNT, 
            tags, now,
            mbean.getThreadCount()));
        metrics.add(new Metric(
            DAEMON_THREAD_COUNT, 
            tags, now,
            mbean.getDaemonThreadCount()));
        metrics.add(new Metric(
            PEAK_THREAD_COUNT, 
            tags, now,
            mbean.getPeakThreadCount()));
    }

    public List<Metric> poll(MetricFilter filter) {
        List<Metric> metrics = Lists.newArrayList();
        getClassLoadingMetrics(metrics, ManagementFactory.getClassLoadingMXBean());
        getCompilationMetrics(metrics, ManagementFactory.getCompilationMXBean());

        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            getGarbageCollectorMetrics(metrics, b);
        }

        getMemoryMetrics(metrics, ManagementFactory.getMemoryMXBean());
        getOperatingSystemMetrics(metrics, ManagementFactory.getOperatingSystemMXBean());
        getThreadMetrics(metrics, ManagementFactory.getThreadMXBean());
        return metrics;
    }
}

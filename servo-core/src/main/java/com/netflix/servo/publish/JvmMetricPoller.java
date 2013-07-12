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
package com.netflix.servo.publish;

import com.google.common.collect.Lists;
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Poller for standard JVM metrics.
 */
public class JvmMetricPoller implements MetricPoller {

    private static final String CLASS = "class";

    private static final MonitorConfig LOADED_CLASS_COUNT =
            MonitorConfig.builder("loadedClassCount")
                    .withTag(CLASS, ClassLoadingMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig TOTAL_LOADED_CLASS_COUNT =
            MonitorConfig.builder("totalLoadedClassCount")
                    .withTag(CLASS, ClassLoadingMXBean.class.getSimpleName())
                    .withTag(DataSourceType.COUNTER)
                    .build();

    private static final MonitorConfig UNLOADED_CLASS_COUNT =
            MonitorConfig.builder("unloadedClassCount")
                    .withTag(CLASS, ClassLoadingMXBean.class.getSimpleName())
                    .withTag(DataSourceType.COUNTER)
                    .build();

    private static final MonitorConfig TOTAL_COMPILATION_TIME =
            MonitorConfig.builder("totalCompilationTime")
                    .withTag(CLASS, CompilationMXBean.class.getSimpleName())
                    .withTag(DataSourceType.COUNTER)
                    .build();

    private static final MonitorConfig COLLECTION_COUNT =
            MonitorConfig.builder("collectionCount")
                    .withTag(CLASS, GarbageCollectorMXBean.class.getSimpleName())
                    .withTag(DataSourceType.COUNTER)
                    .build();

    private static final MonitorConfig COLLECTION_TIME =
            MonitorConfig.builder("collectionTime")
                    .withTag(CLASS, GarbageCollectorMXBean.class.getSimpleName())
                    .withTag(DataSourceType.COUNTER)
                    .build();

    private static final MonitorConfig COMMITTED_USAGE =
            MonitorConfig.builder("committedUsage")
                    .withTag(CLASS, MemoryPoolMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig INIT_USAGE =
            MonitorConfig.builder("initUsage")
                    .withTag(CLASS, MemoryPoolMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig MAX_USAGE =
            MonitorConfig.builder("maxUsage")
                    .withTag(CLASS, MemoryPoolMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig ACTUAL_USAGE =
            MonitorConfig.builder("actualUsage")
                    .withTag(CLASS, MemoryPoolMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig AVAILABLE_PROCESSORS =
            MonitorConfig.builder("availableProcessors")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig LOAD_AVERAGE =
            MonitorConfig.builder("systemLoadAverage")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig MAX_FILE_DESCRIPTOR_COUNT =
            MonitorConfig.builder("maxFileDescriptorCount")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig OPEN_FILE_DESCRIPTOR_COUNT =
            MonitorConfig.builder("openFileDescriptorCount")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig COMMITTED_VIRTUAL_MEMORY_SIZE =
            MonitorConfig.builder("committedVirtualMemorySize")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig TOTAL_PHYSICAL_MEMORY_SIZE =
            MonitorConfig.builder("totalPhysicalMemorySize")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig FREE_PHYSICAL_MEMORY_SIZE =
            MonitorConfig.builder("freePhysicalMemorySize")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig TOTAL_SWAP_SPACE_SIZE =
            MonitorConfig.builder("totalSwapSpaceSize")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig FREE_SWAP_SPACE_SIZE =
            MonitorConfig.builder("freeSwapSpaceSize")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig PROCESS_CPU_LOAD =
            MonitorConfig.builder("processCpuLoad")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig SYSTEM_CPU_LOAD =
            MonitorConfig.builder("systemCpuLoad")
                    .withTag(CLASS, OperatingSystemMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig DAEMON_THREAD_COUNT =
            MonitorConfig.builder("daemonThreadCount")
                    .withTag(CLASS, ThreadMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig THREAD_COUNT =
            MonitorConfig.builder("threadCount")
                    .withTag(CLASS, ThreadMXBean.class.getSimpleName())
                    .withTag(DataSourceType.GAUGE)
                    .build();

    private static final MonitorConfig TOTAL_STARTED_THREAD_COUNT =
            MonitorConfig.builder("totalStartedThreadCount")
                    .withTag(CLASS, ThreadMXBean.class.getSimpleName())
                    .withTag(DataSourceType.COUNTER)
                    .build();

    private static final Logger LOGGER = LoggerFactory.getLogger(JvmMetricPoller.class);

    JvmMetricPoller() {
    }

    @Override
    public final List<Metric> poll(MetricFilter filter) {
        return poll(filter, false);
    }

    @Override
    public final List<Metric> poll(MetricFilter filter, boolean reset) {
        long now = System.currentTimeMillis();
        MetricList metrics = new MetricList(filter);
        addClassLoadingMetrics(now, metrics);
        addCompilationMetrics(now, metrics);
        addGarbageCollectorMetrics(now, metrics);
        addMemoryPoolMetrics(now, metrics);
        addOperatingSystemMetrics(now, metrics);
        addThreadMetrics(now, metrics);
        return metrics.getList();
    }

    private void addClassLoadingMetrics(long timestamp, MetricList metrics) {
        ClassLoadingMXBean bean = ManagementFactory.getClassLoadingMXBean();
        metrics.add(new Metric(LOADED_CLASS_COUNT,
                timestamp, bean.getLoadedClassCount()));
        metrics.add(new Metric(TOTAL_LOADED_CLASS_COUNT,
                timestamp, bean.getTotalLoadedClassCount()));
        metrics.add(new Metric(UNLOADED_CLASS_COUNT,
                timestamp, bean.getUnloadedClassCount()));
    }

    private void addCompilationMetrics(long timestamp, MetricList metrics) {
        CompilationMXBean bean = ManagementFactory.getCompilationMXBean();
        metrics.add(new Metric(TOTAL_COMPILATION_TIME, timestamp, bean.getTotalCompilationTime()));
    }

    private void addGarbageCollectorMetrics(long timestamp, MetricList metrics) {
        final List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : beans) {
            final Tag id = new BasicTag("id", bean.getName());
            metrics.add(new Metric(COLLECTION_COUNT.withAdditionalTag(id),
                    timestamp, bean.getCollectionCount()));
            metrics.add(new Metric(COLLECTION_TIME.withAdditionalTag(id),
                    timestamp, bean.getCollectionTime()));
        }
    }

    private void addMemoryPoolMetrics(long timestamp, MetricList metrics) {
        final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean bean : beans) {
            final TagList tags = BasicTagList.of("id", bean.getName(),
                    "memtype", bean.getType().name());
            addMemoryUsageMetrics(tags, timestamp, bean.getUsage(), metrics);
        }
    }

    private void addMemoryUsageMetrics(
            TagList tags, long timestamp, MemoryUsage usage, MetricList metrics) {
        metrics.add(new Metric(COMMITTED_USAGE.withAdditionalTags(tags),
                timestamp, usage.getCommitted()));
        metrics.add(new Metric(INIT_USAGE.withAdditionalTags(tags), timestamp, usage.getInit()));
        metrics.add(new Metric(ACTUAL_USAGE.withAdditionalTags(tags), timestamp, usage.getUsed()));
        metrics.add(new Metric(MAX_USAGE.withAdditionalTags(tags), timestamp, usage.getMax()));
    }

    private void addOperatingSystemMetrics(long timestamp, MetricList metrics) {
        OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        metrics.add(new Metric(AVAILABLE_PROCESSORS, timestamp, bean.getAvailableProcessors()));
        metrics.add(new Metric(LOAD_AVERAGE, timestamp, bean.getSystemLoadAverage()));
        addOptionalMetric(MAX_FILE_DESCRIPTOR_COUNT,
                timestamp, bean, "getMaxFileDescriptorCount", metrics);
        addOptionalMetric(OPEN_FILE_DESCRIPTOR_COUNT,
                timestamp, bean, "getOpenFileDescriptorCount", metrics);
        addOptionalMetric(COMMITTED_VIRTUAL_MEMORY_SIZE,
                timestamp, bean, "getCommittedVirtualMemorySize", metrics);
        addOptionalMetric(TOTAL_PHYSICAL_MEMORY_SIZE,
                timestamp, bean, "getTotalPhysicalMemorySize", metrics);
        addOptionalMetric(FREE_PHYSICAL_MEMORY_SIZE,
                timestamp, bean, "getFreePhysicalMemorySize", metrics);
        addOptionalMetric(TOTAL_SWAP_SPACE_SIZE,
                timestamp, bean, "getTotalSwapSpaceSize", metrics);
        addOptionalMetric(FREE_SWAP_SPACE_SIZE,
                timestamp, bean, "getFreeSwapSpaceSize", metrics);
        addOptionalMetric(PROCESS_CPU_LOAD,
                timestamp, bean, "getProcessCpuLoad", metrics);
        addOptionalMetric(SYSTEM_CPU_LOAD,
                timestamp, bean, "getSystemCpuLoad", metrics);
    }

    private void addThreadMetrics(long timestamp, MetricList metrics) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        metrics.add(new Metric(DAEMON_THREAD_COUNT, timestamp, bean.getDaemonThreadCount()));
        metrics.add(new Metric(THREAD_COUNT, timestamp, bean.getThreadCount()));
        metrics.add(new Metric(TOTAL_STARTED_THREAD_COUNT,
                timestamp, bean.getTotalStartedThreadCount()));
    }

    private void addOptionalMetric(
            MonitorConfig config,
            long timestamp,
            Object obj,
            String methodName,
            MetricList metrics) {
        try {
            Method method = obj.getClass().getMethod(methodName);
            method.setAccessible(true);
            Number value = (Number) method.invoke(obj);
            metrics.add(new Metric(config, timestamp, value));
        } catch (Exception e) {
            final String msg = String.format("failed to get value for %s.%s",
                    obj.getClass().getName(), methodName);
            LOGGER.debug(msg, e);
        }
    }

    private static class MetricList {
        private final MetricFilter filter;
        private final List<Metric> list;

        public MetricList(MetricFilter filter) {
            this.filter = filter;
            list = Lists.newArrayList();
        }

        public void add(Metric m) {
            if (filter.matches(m.getConfig())) {
                list.add(m);
            }
        }

        public List<Metric> getList() {
            return list;
        }
    }
}

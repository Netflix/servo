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
package com.netflix.servo.util;

import java.io.OutputStream;
import java.io.PrintWriter;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keep track of the cpu usage for threads in the jvm.
 */
public final class ThreadCpuStats {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadCpuStats.class);

    private static final ThreadCpuStats INSTANCE = new ThreadCpuStats();

    private static final long ONE_MINUTE_NANOS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.MINUTES);
    private static final long FIVE_MINUTE_NANOS = 5 * ONE_MINUTE_NANOS;
    private static final long FIFTEEN_MINUTE_NANOS = 15 * ONE_MINUTE_NANOS;

    private volatile boolean running = false;

    private final CpuUsage jvmCpuUsage = new CpuUsage(-1, "jvm");

    private final Map<Long, CpuUsage> threadCpuUsages = new ConcurrentHashMap<Long, CpuUsage>();

    /** Return the singleton instance. */
    public static ThreadCpuStats getInstance() {
        return INSTANCE;
    }

    /** Creates a new instance. */
    private ThreadCpuStats() {
    }

    /** Returns true if cpu status are currently being collected. */
    public boolean isRunning() {
        return false;
    }

    /** Start collecting cpu stats for the threads. */
    public synchronized void start() {
        if (!running) {
            running = true;
            Thread t = new Thread(new CpuStatRunnable(), "ThreadCpuStatsCollector");
            t.start();
        }
    }

    /** Stop collecting cpu stats for the threads. */
    public void stop() {
        running = false;
    }

    /** Overall usage for the jvm. */
    public CpuUsage getOverallCpuUsage() {
        return jvmCpuUsage;
    }

    /** List of cpu usages for each thread. */
    public List<CpuUsage> getThreadCpuUsages() {
        return new ArrayList<CpuUsage>(threadCpuUsages.values());
    }

    /** Helper function for computing percentage. */
    public static double toPercent(long value, long total) {
        return (total > 0) ? 100.0 * value / total : 0.0;
    }

    private static long append(StringBuilder buf, char label, long unit, long time) {
        if (time > unit) {
            long multiple = time / unit;
            buf.append(multiple).append(label);
            return time % unit;
        } else {
            return time;
        }
    }

    /**
     * Convert time in nanoseconds to a duration string. This is used to provide a more human
     * readable order of magnitude for the duration. We assume standard fixed size quantites for
     * all units.
     */
    public static String toDuration(long time) {
        final long second = 1000000000L;
        final long minute = 60 * second;
        final long hour = 60 * minute;
        final long day = 24 * hour;
        final long week = 7 * day;
        final StringBuilder buf = new StringBuilder();
        buf.append('P');
        time = append(buf, 'W', week, time);
        time = append(buf, 'D', day, time);
        buf.append('T');
        time = append(buf, 'H', week, time);
        time = append(buf, 'M', minute, time);
        time = append(buf, 'S', second, time);
        return buf.toString();
    }

    /**
     * Utility function that dumps the cpu usages for the threads to stdout. Output will be sorted
     * based on the 1-minute usage from highest to lowest.
     */
    public void printThreadCpuUsages() {
        printThreadCpuUsages(System.out, CpuUsageComparator.ONE_MINUTE);
    }

    /**
     * Utility function that dumps the cpu usages for the threads to stdout. Output will be sorted
     * based on the 1-minute usage from highest to lowest.
     *
     * @param out  stream where output will be written
     * @param cmp  order to use for the results
     */
    public void printThreadCpuUsages(OutputStream out, CpuUsageComparator cmp) {
        final PrintWriter writer = new PrintWriter(out, true);
        final CpuUsage overall = getOverallCpuUsage();
        final List<CpuUsage> usages = getThreadCpuUsages();
        Collections.sort(usages, cmp);

        writer.printf("Time: %s%n%n", new java.util.Date());

        final long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        final long uptimeNanos = TimeUnit.NANOSECONDS.convert(uptimeMillis, TimeUnit.MILLISECONDS);
        writer.printf("Uptime: %s%n%n", toDuration(uptimeNanos));

        writer.println("JVM Usage Time: ");
        writer.printf("%11s %11s %11s %11s   %7s   %s%n",
            "1-min", "5-min", "15-min", "overall", "id", "name");
        writer.printf("%11s %11s %11s %11s   %7s   %s%n",
            toDuration(overall.getOneMinute()),
            toDuration(overall.getFiveMinute()),
            toDuration(overall.getFifteenMinute()),
            toDuration(overall.getOverall()),
            "-",
            "jvm");
        writer.println();

        final int numProcs = Runtime.getRuntime().availableProcessors();
        writer.println("JVM Usage Percent: ");
        writer.printf("%11s %11s %11s %11s   %7s   %s%n",
            "1-min", "5-min", "15-min", "overall", "id", "name");
        writer.printf("%10.2f%% %10.2f%% %10.2f%% %10.2f%%   %7s   %s%n",
            toPercent(overall.getOneMinute(), ONE_MINUTE_NANOS * numProcs),
            toPercent(overall.getFiveMinute(), FIVE_MINUTE_NANOS * numProcs),
            toPercent(overall.getFifteenMinute(), FIFTEEN_MINUTE_NANOS * numProcs),
            toPercent(overall.getOverall(), uptimeNanos * numProcs),
            "-",
            "jvm");
        writer.println();

        writer.println("Breakdown by thread (100% = total cpu usage for jvm):");
        writer.printf("%11s %11s %11s %11s   %7s   %s%n",
            "1-min", "5-min", "15-min", "overall", "id", "name");
        for (CpuUsage usage : usages) {
            writer.printf("%10.2f%% %10.2f%% %10.2f%% %10.2f%%   %7d   %s%n",
                toPercent(usage.getOneMinute(), overall.getOneMinute()),
                toPercent(usage.getFiveMinute(), overall.getFiveMinute()),
                toPercent(usage.getFifteenMinute(), overall.getFifteenMinute()),
                toPercent(usage.getOverall(), overall.getOverall()),
                usage.getThreadId(),
                usage.getName());
        }
        writer.println();
        writer.flush();
    }

    /** Update the stats for all threads and the jvm. */
    private void updateStats() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean.isThreadCpuTimeEnabled()) {
            // Update stats for all current threads
            final long[] ids = bean.getAllThreadIds();
            Arrays.sort(ids);
            long totalCpuTime = 0L;
            for (int i = 0; i < ids.length; ++i) {
                long cpuTime = bean.getThreadCpuTime(ids[i]);
                if (cpuTime != -1) {
                    totalCpuTime += cpuTime;
                    CpuUsage usage = threadCpuUsages.get(ids[i]);
                    if (usage == null) {
                        final ThreadInfo info = bean.getThreadInfo(ids[i]);
                        usage = new CpuUsage(ids[i], info.getThreadName());
                        threadCpuUsages.put(ids[i], usage);
                    }
                    usage.update(cpuTime);
                }
            }

            // Update jvm cpu usage, if possible we query the operating system mxbean so we get
            // an accurate total including any threads that may have been started and stopped
            // between sampling. As a fallback we use the sum of the cpu time for all threads found
            // in this interval.
            // 
            // Total cpu time can be found in:
            // http://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/OperatingSystemMXBean.html
            //
            // We use reflection to avoid direct dependency on com.sun.* classes.
            final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            try {
                final Method m = osBean.getClass().getMethod("getProcessCpuTime");
                final long jvmCpuTime = (Long) m.invoke(osBean);
                jvmCpuUsage.update((jvmCpuTime < 0) ? totalCpuTime : jvmCpuTime);
            } catch (Exception e) {
                jvmCpuUsage.update(totalCpuTime);
            }

            // Handle ids in the map that no longer exist:
            // * Remove old entries if the last update time is over 15 minutes old
            // * Otherwise, update usage so rolling window is correct
            final long ageLimit = 15 * 60 * 1000;
            final long now = System.currentTimeMillis();
            final Iterator<Map.Entry<Long, CpuUsage>> iter = threadCpuUsages.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<Long, CpuUsage> entry = iter.next();
                final long id = entry.getKey();
                final CpuUsage usage = entry.getValue();
                if (now - usage.getLastUpdateTime() > ageLimit) {
                    iter.remove();
                } else if (Arrays.binarySearch(ids, id) < 0) {
                    usage.updateNoValue();
                }
            }
        } else {
            LOGGER.debug("ThreadMXBean.isThreadCpuTimeEnabled() == false, cannot collect stats");
        }
    }

    /** Update the stats for threads each minute. */
    private class CpuStatRunnable implements Runnable {
        public void run() {
            final long step = 60000L;
            final long maxUpdateTime = step / 6;
            while (running) {
                try {
                    long start = System.currentTimeMillis();
                    updateStats();
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed > maxUpdateTime) {
                        LOGGER.warn("update stats is slow, took {} milliseconds", elapsed);
                    }
                    long delay = step - elapsed;
                    Thread.sleep((delay < 1000L) ? step : delay);
                } catch (Exception e) {
                    LOGGER.warn("failed to update thread stats", e);
                }
            }
        }
    }

    /** Keeps track of the cpu usage for a single thread. */
    public static class CpuUsage {

        private static final int BUFFER_SIZE = 16;

        private final long id;
        private final String name;

        private final AtomicLong lastUpdateTime = new AtomicLong(0L);

        private final AtomicInteger nextPos = new AtomicInteger(0);

        /** Cumulative cpu times for the different intervals. */
        private AtomicLongArray totals = new AtomicLongArray(BUFFER_SIZE);

        private CpuUsage(long id, String name) {
            this.id = id;
            this.name = name;
        }

        /**
         * The thread id that is being tracked. If the id is less than 0 it is for the jvm process
         * rather than a single thread.
         */
        public long getThreadId() {
            return id;
        }

        /** Name of the thread. */
        public String getName() {
            return name;
        }

        /** Last time the stats for this object were updated. */
        public long getLastUpdateTime() {
            return lastUpdateTime.get();
        }

        /** Returns the overall usage for the lifetime of the thread. */
        public long getOverall() {
            final int currentPos = toIndex(nextPos.get() - 1);
            return totals.get(currentPos);
        }

        /** Returns the usage for the last one minute. */
        public long getOneMinute() {
            return get(1);
        }

        /** Returns the usage for the last five minutes. */
        public long getFiveMinute() {
            return get(5);
        }

        /** Returns the usage for the last fifteen minutes. */
        public long getFifteenMinute() {
            return get(15);
        }

        private int toIndex(int v) {
            return ((v < 0) ? v + BUFFER_SIZE : v) % BUFFER_SIZE;
        }

        private long get(int n) {
            final int currentPos = toIndex(nextPos.get() - 1);
            final int startPos = toIndex(currentPos - n);
            final long currentValue = totals.get(currentPos);
            final long startValue = totals.get(startPos);
            final long diff = currentValue - startValue;
            return (diff < 0L) ? 0L : diff;
        }

        private void update(long threadTotal) {
            totals.set(toIndex(nextPos.getAndIncrement()), threadTotal);
            lastUpdateTime.set(System.currentTimeMillis());
        }

        /**
         * Called if the thread no longer exists. The totals are cumulative so we copy the last
         * previously captured value.
         */
        private void updateNoValue() {
            final int currentPos = toIndex(nextPos.get() - 1);
            update(totals.get(currentPos));
        }
    }

    /** Comparator for sorting cpu usage based on one of the columns. */
    public static enum CpuUsageComparator implements Comparator<CpuUsage> {
        ONE_MINUTE(0),
        FIVE_MINUTE(1),
        FIFTEEN_MINUTE(2),
        OVERALL(3);

        private final int col;

        private CpuUsageComparator(int col) {
            this.col = col;
        }

        /** {@inheritDoc} */
        public int compare(CpuUsage u1, CpuUsage u2) {
            long cmp = 0;
            switch (col) {
                case 0: cmp = u2.getOneMinute() - u1.getOneMinute(); break;
                case 1: cmp = u2.getFiveMinute() - u1.getFiveMinute(); break;
                case 2: cmp = u2.getFifteenMinute() - u1.getFifteenMinute(); break;
                default: cmp = u2.getOverall() - u1.getOverall(); break;
            }
            return (cmp < 0) ? -1 : ((cmp > 0) ? 1 : 0);
        }
    }
}

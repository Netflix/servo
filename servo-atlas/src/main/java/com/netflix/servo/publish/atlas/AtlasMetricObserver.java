/**
 * Copyright 2015 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.publish.atlas;

import com.netflix.archaius.config.EmptyConfig;
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicGauge;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Gauge;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.Pollers;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import iep.com.netflix.iep.http.BasicServerRegistry;
import iep.com.netflix.iep.http.RxHttp;
import iep.io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Observer that forwards metrics to atlas. In addition to being MetricObserver, it also supports
 * a push model that sends metrics as soon as possible (asynchronously).
 */
public class AtlasMetricObserver implements MetricObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(AtlasMetricObserver.class);
  private static final Tag ATLAS_COUNTER_TAG = new BasicTag("atlas.dstype", "counter");
  private static final Tag ATLAS_GAUGE_TAG = new BasicTag("atlas.dstype", "gauge");
  private static final UpdateTasks NO_TASKS = new UpdateTasks(0, null, -1L);
  protected final HttpHelper httpHelper;
  protected final ServoAtlasConfig config;
  protected final long sendTimeoutMs; // in milliseconds
  protected final long stepMs; // in milliseconds
  private final Counter numMetricsTotal = Monitors.newCounter("numMetricsTotal");
  private final Timer updateTimer = Monitors.newTimer("update");
  private final Counter numMetricsDroppedSendTimeout = newErrCounter("numMetricsDropped",
      "sendTimeout");
  private final Counter numMetricsDroppedQueueFull = newErrCounter("numMetricsDropped",
      "sendQueueFull");
  private final Counter numMetricsDroppedHttpErr = newErrCounter("numMetricsDropped",
      "httpError");
  private final Counter numMetricsSent = Monitors.newCounter("numMetricsSent");
  private final TagList commonTags;
  private final BlockingQueue<UpdateTasks> pushQueue;
  @SuppressWarnings("unused")
  private final Gauge<Integer> pushQueueSize = new BasicGauge<>(
      MonitorConfig.builder("pushQueue").build(), new Callable<Integer>() {
    @Override
    public Integer call() throws Exception {
      return pushQueue.size();
    }
  });

  /**
   * Create an observer that can send metrics to atlas with a given
   * config and list of common tags.
   * This method will use the default poller index of 0.
   */
  public AtlasMetricObserver(ServoAtlasConfig config, TagList commonTags) {
    this(config, commonTags, 0);
  }

  /**
   * Create an observer that can send metrics to atlas with a given config, list of common tags,
   * and poller index.
   */
  public AtlasMetricObserver(ServoAtlasConfig config, TagList commonTags, int pollerIdx) {
    this(config, commonTags, pollerIdx, new HttpHelper(new RxHttp(EmptyConfig.INSTANCE, new BasicServerRegistry())));
  }

  /**
   * Create an atlas observer. For internal use of servo only.
   */
  public AtlasMetricObserver(ServoAtlasConfig config,
                             TagList commonTags,
                             int pollerIdx,
                             HttpHelper httpHelper) {
    this.httpHelper = httpHelper;
    this.config = config;
    this.stepMs = Pollers.getPollingIntervals().get(pollerIdx);
    this.sendTimeoutMs = stepMs * 9 / 10;
    this.commonTags = commonTags;
    pushQueue = new LinkedBlockingQueue<>(config.getPushQueueSize());
    final Thread pushThread = new Thread(new PushProcessor(), "BaseAtlasMetricObserver-Push");
    pushThread.setDaemon(true);
    pushThread.start();
  }

  protected static Counter newErrCounter(String name, String err) {
    return new BasicCounter(MonitorConfig.builder(name).withTag("error", err).build());
  }

  protected static Metric asGauge(Metric m) {
    return new Metric(m.getConfig().withAdditionalTag(ATLAS_GAUGE_TAG),
        m.getTimestamp(), m.getValue());
  }

  protected static Metric asCounter(Metric m) {
    return new Metric(m.getConfig().withAdditionalTag(ATLAS_COUNTER_TAG),
        m.getTimestamp(), m.getValue());
  }

  protected static boolean isCounter(Metric m) {
    final TagList tags = m.getConfig().getTags();
    final String value = tags.getValue(DataSourceType.KEY);
    return value != null && value.equals(DataSourceType.COUNTER.name());
  }

  protected static boolean isGauge(Metric m) {
    final TagList tags = m.getConfig().getTags();
    final String value = tags.getValue(DataSourceType.KEY);
    return value != null && value.equals(DataSourceType.GAUGE.name());
  }

  protected static boolean isRate(Metric m) {
    final TagList tags = m.getConfig().getTags();
    final String value = tags.getValue(DataSourceType.KEY);
    return DataSourceType.RATE.name().equals(value)
        || DataSourceType.NORMALIZED.name().equals(value);
  }

  protected static List<Metric> identifyDsTypes(List<Metric> metrics) {
    // since we never generate atlas.dstype = counter we can do the following:
    return metrics.stream().map(m -> isRate(m) ? m : asGauge(m)).collect(Collectors.toList());
  }

  @Override
  public String getName() {
    return "atlas";
  }

  private List<Metric> identifyCountersForPush(List<Metric> metrics) {
    List<Metric> transformed = new ArrayList<>(metrics.size());
    for (Metric m : metrics) {
      Metric toAdd = m;
      if (isCounter(m)) {
        toAdd = asCounter(m);
      } else if (isGauge(m)) {
        toAdd = asGauge(m);
      }
      transformed.add(toAdd);
    }
    return transformed;
  }

  /**
   * Immediately send metrics to the backend.
   *
   * @param rawMetrics Metrics to be sent. Names and tags will be sanitized.
   */
  public void push(List<Metric> rawMetrics) {
    List<Metric> validMetrics = ValidCharacters.toValidValues(filter(rawMetrics));
    List<Metric> metrics = transformMetrics(validMetrics);

    LOGGER.debug("Scheduling push of {} metrics", metrics.size());
    final UpdateTasks tasks = getUpdateTasks(BasicTagList.EMPTY,
        identifyCountersForPush(metrics));
    final int maxAttempts = 5;
    int attempts = 1;
    while (!pushQueue.offer(tasks) && attempts <= maxAttempts) {
      ++attempts;
      final UpdateTasks droppedTasks = pushQueue.remove();
      LOGGER.warn("Removing old push task due to queue full. Dropping {} metrics.",
          droppedTasks.numMetrics);
      numMetricsDroppedQueueFull.increment(droppedTasks.numMetrics);
    }
    if (attempts >= maxAttempts) {
      LOGGER.error("Unable to push update of {}", tasks);
      numMetricsDroppedQueueFull.increment(tasks.numMetrics);
    } else {
      LOGGER.debug("Queued push of {}", tasks);
    }
  }

  protected void sendNow(UpdateTasks updateTasks) {
    if (updateTasks.numMetrics == 0) {
      return;
    }

    final Stopwatch s = updateTimer.start();
    int totalSent = 0;
    try {
      totalSent = httpHelper.sendAll(updateTasks.tasks,
          updateTasks.numMetrics, sendTimeoutMs);
      LOGGER.debug("Sent {}/{} metrics to atlas", totalSent, updateTasks.numMetrics);
    } finally {
      s.stop();
      int dropped = updateTasks.numMetrics - totalSent;
      numMetricsDroppedSendTimeout.increment(dropped);
    }
  }

  protected boolean shouldIncludeMetric(Metric metric) {
    return true;
  }

  /**
   * Return metrics to be sent to the main atlas deployment.
   * Metrics will be sent if their publishing policy matches atlas and if they
   * will *not* be sent to the aggregation cluster.
   */
  protected List<Metric> filter(List<Metric> metrics) {
    final List<Metric> filtered = metrics.stream().filter(this::shouldIncludeMetric)
        .collect(Collectors.toList());
    LOGGER.debug("Filter: input {} metrics, output {} metrics",
        metrics.size(), filtered.size());
    return filtered;
  }

  protected List<Metric> transformMetrics(List<Metric> metrics) {
    return metrics;
  }

  @Override
  public void update(List<Metric> rawMetrics) {
    List<Metric> valid = ValidCharacters.toValidValues(rawMetrics);
    List<Metric> metrics = identifyDsTypes(filter(valid));
    List<Metric> transformed = transformMetrics(metrics);
    sendNow(getUpdateTasks(getCommonTags(), transformed));
  }

  private UpdateTasks getUpdateTasks(TagList tags, List<Metric> metrics) {
    if (!config.shouldSendMetrics()) {
      LOGGER.debug("Plugin disabled or running on a dev environment. Not sending metrics.");
      return NO_TASKS;
    }

    if (metrics.isEmpty()) {
      LOGGER.debug("metrics list is empty, no data being sent to server");
      return NO_TASKS;
    }

    final int numMetrics = metrics.size();
    final Metric[] atlasMetrics = new Metric[metrics.size()];
    metrics.toArray(atlasMetrics);

    numMetricsTotal.increment(numMetrics);
    final List<Observable<Integer>> tasks = new ArrayList<>();
    final String uri = config.getAtlasUri();
    LOGGER.debug("writing {} metrics to atlas ({})", numMetrics, uri);
    int i = 0;
    while (i < numMetrics) {
      final int remaining = numMetrics - i;
      final int batchSize = Math.min(remaining, config.batchSize());
      final Metric[] batch = new Metric[batchSize];
      System.arraycopy(atlasMetrics, i, batch, 0, batchSize);
      final Observable<Integer> sender = getSenderObservable(tags, batch);
      tasks.add(sender);
      i += batchSize;
    }
    assert i == numMetrics;
    LOGGER.debug("succeeded in creating {} observable(s) to send metrics with total size {}",
        tasks.size(), numMetrics);

    return new UpdateTasks(numMetrics * getNumberOfCopies(), tasks,
        System.currentTimeMillis());
  }

  protected int getNumberOfCopies() {
    return 1;
  }

  protected Observable<Integer> getSenderObservable(TagList tags, Metric[] batch) {
    JsonPayload payload = new UpdateRequest(tags, batch, batch.length);
    return httpHelper.postSmile(config.getAtlasUri(), payload)
        .map(withBookkeeping(batch.length));
  }

  /**
   * Get the list of common tags that will be added to all metrics sent by this Observer.
   */
  protected TagList getCommonTags() {
    return commonTags;
  }

  /**
   * Utility function to map an Observable&lt;ByteBuf> to an Observable&lt;Integer> while also
   * updating our counters for metrics sent and errors.
   */
  protected Func1<HttpClientResponse<ByteBuf>, Integer> withBookkeeping(final int batchSize) {
    return response -> {
      boolean ok = response.getStatus().code() == 200;
      if (ok) {
        numMetricsSent.increment(batchSize);
      } else {
        LOGGER.info("Status code: {} - Lost {} metrics",
            response.getStatus().code(), batchSize);
        numMetricsDroppedHttpErr.increment(batchSize);
      }

      return batchSize;
    };
  }

  private static class UpdateTasks {
    private final int numMetrics;
    private final List<Observable<Integer>> tasks;
    private final long timestamp;

    UpdateTasks(int numMetrics, List<Observable<Integer>> tasks, long timestamp) {
      this.numMetrics = numMetrics;
      this.tasks = tasks;
      this.timestamp = timestamp;
    }

    @Override
    public String toString() {
      return "UpdateTasks{numMetrics=" + numMetrics + ", tasks="
          + tasks + ", timestamp=" + timestamp + '}';
    }
  }

  private class PushProcessor implements Runnable {
    @Override
    public void run() {
      boolean interrupted = false;
      while (!interrupted) {
        try {
          sendNow(pushQueue.take());
        } catch (InterruptedException e) {
          LOGGER.debug("Interrupted trying to get next UpdateTask to push");
          interrupted = true;
        } catch (Exception t) {
          LOGGER.info("Caught unexpected exception pushing metrics", t);
        }
      }
    }
  }

}

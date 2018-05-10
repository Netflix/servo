/*
 * Copyright 2011-2018 Netflix, Inc.
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
package com.netflix.servo.monitor;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.SpectatorContext;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.stats.StatsConfig;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ManualClock;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Statistic;
import com.netflix.spectator.api.patterns.PolledMeter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class SpectatorIntegrationTest {

  private static final MonitorConfig CONFIG = new MonitorConfig.Builder("test").build();
  private static final Id ID = new DefaultRegistry()
      .createId(CONFIG.getName())
      .withTags(CONFIG.getTags().asMap());

  private Registry registry;

  @BeforeMethod
  public void before() {
    registry = new DefaultRegistry();
    SpectatorContext.setRegistry(registry);
  }

  @Test
  public void testBasicCounterIncrement() {
    BasicCounter c = new BasicCounter(CONFIG);
    c.increment();
    assertEquals(1, registry.counter(ID).count());
  }

  @Test
  public void testBasicCounterIncrementAmount() {
    BasicCounter c = new BasicCounter(CONFIG);
    c.increment(42);
    assertEquals(42, registry.counter(ID).count());
  }

  @Test
  public void testStepCounterIncrement() {
    BasicCounter c = new BasicCounter(CONFIG);
    c.increment();
    assertEquals(1, registry.counter(ID).count());
  }

  @Test
  public void testStepCounterIncrementAmount() {
    BasicCounter c = new BasicCounter(CONFIG);
    c.increment(42);
    assertEquals(42, registry.counter(ID).count());
  }

  @Test
  public void testDynamicCounterIncrement() {
    DynamicCounter.increment(CONFIG);
    assertEquals(1, registry.counter(ID).count());
  }

  @Test
  public void testDoubleCounterAdd() {
    DoubleCounter c = new DoubleCounter(CONFIG, Clock.WALL);
    c.increment(0.2);
    assertEquals(0.2, registry.counter(ID).actualCount());
  }

  @Test
  public void testAnnotatedCounter() {
    AnnotateExample ex = new AnnotateExample("foo");
    PolledMeter.update(registry);
    Id id = registry.createId("counter")
        .withTag("class", "AnnotateExample")
        .withTag("level", "INFO")
        .withTag("id", "foo")
        .withTag("type", "COUNTER");
    assertEquals(1, registry.counter(id).count());
  }

  @Test
  public void testContextualCounter() {
    TagList context = BasicTagList.of("a", "1");
    ContextualCounter c = new ContextualCounter(CONFIG, () -> context, BasicCounter::new);
    c.increment();
    Id id = ID.withTag("a", "1");
    assertEquals(1, registry.counter(id).count());
  }

  @Test
  public void testPeakRateCounter() {
    PeakRateCounter c = new PeakRateCounter(CONFIG);
    DefaultMonitorRegistry.getInstance().register(c);
    c.increment();
    PolledMeter.update(registry);
    registry.stream().forEach(m -> System.out.println(m.id()));
    assertEquals(1.0, registry.gauge(ID.withTag("type", "GAUGE")).value());
  }

  @Test
  public void testPeakRateCounterRemove() {
    PeakRateCounter c = new PeakRateCounter(CONFIG);
    DefaultMonitorRegistry.getInstance().register(c);
    DefaultMonitorRegistry.getInstance().unregister(c);
    c.increment();
    PolledMeter.update(registry);
    assertEquals(0, registry.stream().count());
  }

  @Test
  public void testDoubleGauge() {
    DoubleGauge c = new DoubleGauge(CONFIG);
    c.set(42.0);
    assertEquals(42.0, registry.gauge(ID).value(), 1e-12);
  }

  @Test
  public void testNumberGauge() {
    Number n = 42.0;
    NumberGauge c = new NumberGauge(CONFIG, n);
    PolledMeter.update(registry);
    assertEquals(42.0, registry.gauge(ID).value(), 1e-12);
  }

  @Test
  public void testBasicGauge() {
    BasicGauge<Double> c = new BasicGauge<>(CONFIG, () -> 42.0);
    PolledMeter.update(registry);
    assertEquals(42.0, registry.gauge(ID).value(), 1e-12);
  }

  @Test
  public void testAnnotatedGauge() {
    AnnotateExample ex = new AnnotateExample("foo");
    PolledMeter.update(registry);
    Id id = registry.createId("gauge")
        .withTag("class", "AnnotateExample")
        .withTag("level", "INFO")
        .withTag("id", "foo")
        .withTag("type", "GAUGE");
    assertEquals(42.0, registry.gauge(id).value(), 1e-12);
  }

  @Test
  public void testDynamicGauge() {
    DynamicGauge.set(CONFIG, 42.0);
    assertEquals(42.0, registry.gauge(ID).value(), 1e-12);
  }

  @Test
  public void testDoubleMaxGauge() {
    DoubleGauge c = new DoubleGauge(CONFIG);
    c.set(42.0);
    assertEquals(42.0, registry.maxGauge(ID).value(), 1e-12);
  }

  @Test
  public void testMinGauge() {
    ManualClock clock = new ManualClock(0);
    MinGauge g = new MinGauge(CONFIG, clock);
    DefaultMonitorRegistry.getInstance().register(g);
    g.update(42);
    clock.set(60000);
    PolledMeter.update(registry);
    assertEquals(42.0, registry.gauge(ID.withTag("type", "GAUGE")).value());
  }

  @Test
  public void testMinGaugeRemove() {
    MinGauge g = new MinGauge(CONFIG);
    DefaultMonitorRegistry.getInstance().register(g);
    DefaultMonitorRegistry.getInstance().unregister(g);
    g.update(42);
    PolledMeter.update(registry);
    assertEquals(0, registry.stream().count());
  }

  @Test
  public void testBasicDistributionSummaryRecord() {
    BasicDistributionSummary d = new BasicDistributionSummary(CONFIG);
    d.record(42);
    assertEquals(1, registry.counter(ID.withTag(Statistic.count)).count());
    assertEquals(42, registry.counter(ID.withTag(Statistic.totalAmount)).count());
    assertEquals(42.0, registry.maxGauge(ID.withTag(Statistic.max)).value(), 1e-12);
  }

  @Test
  public void testBasicTimerRecordMillis() {
    BasicTimer d = new BasicTimer(CONFIG);
    d.record(42, TimeUnit.NANOSECONDS);
    Id id = ID.withTag("unit", "MILLISECONDS");
    assertEquals(1, registry.counter(id.withTag(Statistic.count)).count());
    assertEquals(42e-6, registry.counter(id.withTag(Statistic.totalTime)).actualCount(), 1e-12);
    assertEquals(42e-6 * 42e-6, registry.counter(id.withTag(Statistic.totalOfSquares)).actualCount(), 1e-12);
    assertEquals(42e-6, registry.maxGauge(id.withTag(Statistic.max)).value(), 1e-12);
  }

  @Test
  public void testBasicTimerRecordSeconds() {
    BasicTimer d = new BasicTimer(CONFIG, TimeUnit.SECONDS);
    d.record(42, TimeUnit.NANOSECONDS);
    Id id = ID.withTag("unit", "SECONDS");
    assertEquals(1, registry.counter(id.withTag(Statistic.count)).count());
    assertEquals(42e-9, registry.counter(id.withTag(Statistic.totalTime)).actualCount(), 1e-12);
    assertEquals(42e-9 * 42e-9, registry.counter(id.withTag(Statistic.totalOfSquares)).actualCount(), 1e-12);
    assertEquals(42e-9, registry.maxGauge(id.withTag(Statistic.max)).value(), 1e-12);
  }

  @Test
  public void testDynamicTimerRecordSeconds() {
    DynamicTimer.record(CONFIG, 42);
    Id id = ID.withTag("unit", "MILLISECONDS");
    assertEquals(1, registry.counter(id.withTag(Statistic.count)).count());
    assertEquals(42, registry.counter(id.withTag(Statistic.totalTime)).actualCount(), 1e-12);
    assertEquals(42 * 42, registry.counter(id.withTag(Statistic.totalOfSquares)).actualCount(), 1e-12);
    assertEquals(42, registry.maxGauge(id.withTag(Statistic.max)).value(), 1e-12);
  }

  @Test
  public void testBucketTimerRecordMillis() {
    BucketConfig bc = new BucketConfig.Builder()
        .withBuckets(new long[] {10L, 50L})
        .withTimeUnit(TimeUnit.MILLISECONDS)
        .build();
    BucketTimer d = new BucketTimer(CONFIG, bc);
    d.record(42, TimeUnit.MILLISECONDS);
    Id id = ID.withTag("unit", "MILLISECONDS");
    assertEquals(1, registry.counter(id.withTag(Statistic.count).withTag("servo.bucket", "bucket=50ms")).count());
    assertEquals(42.0, registry.counter(id.withTag(Statistic.totalTime)).actualCount(), 1e-12);
    assertEquals(42.0, registry.maxGauge(id.withTag(Statistic.max)).value(), 1e-12);
  }

  @Test
  public void testStatsTimerRecordMillis() {
    StatsConfig sc = new StatsConfig.Builder()
        .withPercentiles(new double[] {50.0, 95.0})
        .withPublishCount(true)
        .withPublishMax(true)
        .withPublishMean(true)
        .withSampleSize(10)
        .build();
    StatsTimer d = new StatsTimer(CONFIG, sc);
    d.record(42, TimeUnit.MILLISECONDS);
    d.computeStats();
    Id id = ID.withTag("unit", "MILLISECONDS");
    assertEquals(1, registry.counter(id.withTag(Statistic.count)).count());
    assertEquals(42.0, registry.counter(id.withTag(Statistic.totalTime)).actualCount(), 1e-12);
    assertEquals(42.0, registry.maxGauge(id.withTag(Statistic.max)).value(), 1e-12);
    assertEquals(42.0, registry.gauge(id.withTag("statistic", "percentile_50")).value(), 1e-12);
    assertEquals(42.0, registry.gauge(id.withTag("statistic", "percentile_95")).value(), 1e-12);
    assertEquals(42.0, registry.gauge(id.withTag("statistic", "avg")).value(), 1e-12);
  }

  @Test
  public void testContextualTimerRecordMillis() {
    TagList context = BasicTagList.of("a", "1");
    ContextualTimer d = new ContextualTimer(CONFIG, () -> context, BasicTimer::new);
    d.record(42, TimeUnit.NANOSECONDS);
    Id id = ID.withTag("unit", "MILLISECONDS").withTag("a", "1");
    assertEquals(1, registry.counter(id.withTag(Statistic.count)).count());
    assertEquals(42e-6, registry.counter(id.withTag(Statistic.totalTime)).actualCount(), 1e-12);
    assertEquals(42e-6 * 42e-6, registry.counter(id.withTag(Statistic.totalOfSquares)).actualCount(), 1e-12);
    assertEquals(42e-6, registry.maxGauge(id.withTag(Statistic.max)).value(), 1e-12);
  }

  public static class AnnotateExample {

    private long count = 0;

    private final BasicCounter c = new BasicCounter(CONFIG);

    public AnnotateExample(String id) {
      Monitors.registerObject(id, this);
    }

    @Monitor(name = "gauge", type = DataSourceType.GAUGE)
    private double gauge() {
      return 42.0;
    }

    @Monitor(name = "counter", type = DataSourceType.COUNTER)
    private long counter() {
      return count++;
    }
  }
}

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
import com.netflix.spectator.api.BasicTag;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Statistic;
import com.netflix.spectator.api.Tag;
import com.netflix.spectator.api.patterns.PolledMeter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class SpectatorIntegrationTest {

  private static final MonitorConfig CONFIG = new MonitorConfig.Builder("test").build();
  private static final Id ID = new DefaultRegistry()
      .createId(CONFIG.getName())
      .withTags(CONFIG.getTags().asMap());

  private static final Tag COUNTER = new BasicTag("type", "COUNTER");

  private Registry registry;

  @BeforeMethod
  public void before() {
    DefaultMonitorRegistry.getInstance().getRegisteredMonitors().forEach(
        m -> DefaultMonitorRegistry.getInstance().unregister(m)
    );
    registry = new DefaultRegistry();
    SpectatorContext.setRegistry(registry);
  }

  private void register(com.netflix.servo.monitor.Monitor<?> monitor) {
    DefaultMonitorRegistry.getInstance().register(monitor);
  }

  @Test
  public void testUnregisteredBasicCounter() {
    BasicCounter c = new BasicCounter(CONFIG);
    assertEquals(0, registry.counters().count());
  }

  @Test
  public void testUnregisteredBasicCounterIncrement() {
    BasicCounter c = new BasicCounter(CONFIG);
    c.increment();
    assertEquals(1, registry.counters().count());
    assertEquals(1, registry.counter("test").count());
  }

  @Test
  public void testUnregisteredBasicTimer() {
    BasicTimer t = new BasicTimer(CONFIG);
    assertEquals(0, registry.timers().count());
  }

  @Test
  public void testUnregisteredBasicTimerIncrement() {
    BasicTimer t = new BasicTimer(CONFIG);
    t.record(42, TimeUnit.MILLISECONDS);

    Id id = registry.createId("test")
        .withTag("unit", "MILLISECONDS");

    assertEquals(3, registry.counters().count());
    assertEquals(0, registry.timers().count());
    assertEquals(1, registry.gauges().count());
    assertEquals(0, registry.distributionSummaries().count());

    assertEquals(1, registry.counter(id.withTag(Statistic.count)).count());
    assertEquals(42, registry.counter(id.withTag(Statistic.totalTime)).count());
    assertEquals(42 * 42, registry.counter(id.withTag(Statistic.totalOfSquares)).count());
    assertEquals(42.0, registry.maxGauge(id.withTag(Statistic.max)).value());
  }

  @Test
  public void testBasicCounterIncrement() {
    BasicCounter c = new BasicCounter(CONFIG);
    register(c);
    c.increment();
    assertEquals(1, registry.counter(ID).count());
  }

  @Test
  public void testBasicCounterIncrementAmount() {
    BasicCounter c = new BasicCounter(CONFIG);
    register(c);
    c.increment(42);
    assertEquals(42, registry.counter(ID).count());
  }

  @Test
  public void testStepCounterIncrement() {
    StepCounter c = new StepCounter(CONFIG);
    register(c);
    c.increment();
    assertEquals(1, registry.counter(ID).count());
  }

  @Test
  public void testStepCounterIncrementAmount() {
    StepCounter c = new StepCounter(CONFIG);
    register(c);
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
    register(c);
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
    assertEquals(1, registry.counter(id.withTag(COUNTER)).count());
  }

  @Test
  public void testMemberCounter() {
    AnnotateExample ex = new AnnotateExample("foo");
    ex.update();
    Id id = registry.createId("test")
        .withTag("class", "AnnotateExample")
        .withTag("id", "foo");
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
  public void testContextualMemberCounter() {
    ContextualExample c = new ContextualExample("foo");
    c.update();
    Id id = registry.createId("counter")
        .withTag("a", "2")
        .withTag("id", "foo")
        .withTag("class", "ContextualExample");
    assertEquals(1, registry.counter(id).count());
  }

  @Test
  public void testCustomCompositeMemberCounter() {
    CustomCompositeExample c = new CustomCompositeExample("foo");
    c.update("2");
    Id id = registry.createId("test").withTag("c", "2");
    assertEquals(1, registry.counter(id).count());
  }

  @Test
  public void testPeakRateCounter() {
    PeakRateCounter c = new PeakRateCounter(CONFIG);
    DefaultMonitorRegistry.getInstance().register(c);
    c.increment();
    PolledMeter.update(registry);
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
    register(c);
    c.set(42.0);
    assertEquals(42.0, registry.gauge(ID).value(), 1e-12);
  }

  @Test
  public void testNumberGauge() {
    Number n = 42.0;
    NumberGauge c = new NumberGauge(CONFIG, n);
    register(c);
    PolledMeter.update(registry);
    assertEquals(42.0, registry.gauge(ID).value(), 1e-12);
  }

  @Test
  public void testBasicGauge() {
    BasicGauge<Double> c = new BasicGauge<>(CONFIG, () -> 42.0);
    register(c);
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
    register(c);
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
    register(d);
    d.record(42);
    assertEquals(1, registry.counter(ID.withTag(Statistic.count)).count());
    assertEquals(42, registry.counter(ID.withTag(Statistic.totalAmount)).count());
    assertEquals(42.0, registry.maxGauge(ID.withTag(Statistic.max)).value(), 1e-12);
  }

  @Test
  public void testBasicTimerRecordMillis() {
    BasicTimer d = new BasicTimer(CONFIG);
    register(d);
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
    register(d);
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
    register(d);
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
    register(d);
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

    public void update() {
      c.increment();
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

  public static class ContextualExample {
    private final TagList context = BasicTagList.of("a", "2");

    private final ContextualCounter c = new ContextualCounter(
        new MonitorConfig.Builder("counter").build(),
        () -> context,
        BasicCounter::new
    );

    private final ContextualTimer t = new ContextualTimer(
        new MonitorConfig.Builder("timer").build(),
        () -> context,
        BasicTimer::new
    );

    public ContextualExample(String id) {
      Monitors.registerObject(id, this);
    }

    public void update() {
      c.increment();
      t.record(42, TimeUnit.NANOSECONDS);
    }
  }

  public static class CustomCompositeExample {

    private final DynCounter c = new DynCounter(CONFIG);

    public CustomCompositeExample(String id) {
      Monitors.registerObject(id, this);
    }

    public void update(String v) {
      c.increment(v);
    }
  }

  public static class DynCounter extends AbstractMonitor<Long> implements CompositeMonitor<Long> {

    private final Map<String, BasicCounter> counters = new HashMap<>();
    private final MonitorConfig baseConfig;

    public DynCounter(MonitorConfig baseConfig) {
      super(baseConfig);
      this.baseConfig = baseConfig;
    }

    public void increment(String value) {
      counters.computeIfAbsent(value, v -> {
        MonitorConfig c = baseConfig.withAdditionalTag(new com.netflix.servo.tag.BasicTag("c", v));
        return new BasicCounter(c);
      }).increment();
    }

    @Override
    public List<com.netflix.servo.monitor.Monitor<?>> getMonitors() {
      return new ArrayList<>(counters.values());
    }

    @Override
    public Long getValue(int pollerIndex) {
      return 0L;
    }
  }
}

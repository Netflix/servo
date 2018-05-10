package com.netflix.servo.monitor;

import com.netflix.servo.SpectatorContext;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.util.Clock;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Statistic;
import com.netflix.spectator.api.patterns.PolledMeter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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

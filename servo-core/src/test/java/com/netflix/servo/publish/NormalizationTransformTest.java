/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.servo.publish;

import com.netflix.servo.Metric;
import com.netflix.servo.monitor.AbstractMonitor;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.LongGauge;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.StepCounter;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ManualClock;
import com.netflix.servo.util.UnmodifiableList;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class NormalizationTransformTest {
  Metric newMetric(long t, double v) {
    return new Metric(MonitorConfig.builder("test").build(), t, v);
  }

  static class TimeVal {
    final long t;
    final double v;

    static TimeVal from(long t, double v) {
      return new TimeVal(t, v);
    }

    static TimeVal from(Metric m) {
      return new TimeVal(m.getTimestamp(), m.getNumberValue().doubleValue());
    }

    TimeVal(long t, double v) {
      this.t = t;
      this.v = v;
    }

    @Override
    public String toString() {
      return "TimeVal{t=" + t + ", v=" + v + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TimeVal timeVal = (TimeVal) o;
      return t == timeVal.t && Double.compare(timeVal.v, v) == 0;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      result = (int) (t ^ (t >>> 32));
      temp = Double.doubleToLongBits(v);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }
  }

  void assertMetrics(long step, long heartbeat, List<Metric> input, List<TimeVal> expected) {
    ManualClock clock = new ManualClock(0);
    MemoryMetricObserver mmo = new MemoryMetricObserver("m", 1);
    MetricObserver transform = new NormalizationTransform(mmo, step, heartbeat,
        TimeUnit.MILLISECONDS, clock);

    int i = 0;
    for (Metric m : input) {
      transform.update(UnmodifiableList.of(m));
      Metric result = mmo.getObservations().get(0).get(0);
      assertEquals(TimeVal.from(result), expected.get(i));
      i++;
    }
  }

  @Test
  public void testBasic() throws Exception {
    List<Metric> inputList = UnmodifiableList.of(
        newMetric(5, 1.0),
        newMetric(15, 2.0),
        newMetric(25, 2.0),
        newMetric(35, 1.0),
        newMetric(85, 1.0),
        newMetric(95, 2.0),
        newMetric(105, 2.0));
    List<TimeVal> expected = UnmodifiableList.of(
        TimeVal.from(0, 0.5),
        TimeVal.from(10, 1.5),
        TimeVal.from(20, 2.0),
        TimeVal.from(30, 1.5),
        TimeVal.from(80, 0.5),
        TimeVal.from(90, 1.5),
        TimeVal.from(100, 2.0)
    );

    assertMetrics(10, 20, inputList, expected);
  }

  @Test
  public void testAlreadyNormalized() throws Exception {
    List<Metric> inputList = UnmodifiableList.of(
        newMetric(0, 10.0),
        newMetric(10, 20.0),
        newMetric(20, 30.0),
        newMetric(30, 10.0));
    List<TimeVal> expected = UnmodifiableList.of(
        TimeVal.from(0, 10.0),
        TimeVal.from(10, 20.0),
        TimeVal.from(20, 30.0),
        TimeVal.from(30, 10.0));
    assertMetrics(10, 20, inputList, expected);
  }

  @Test
  public void testNormalizedMissedHeartbeat() throws Exception {
    List<Metric> inputList = UnmodifiableList.of(
        newMetric(0, 10.0),
        newMetric(10, 10.0),
        newMetric(30, 10.0));
    List<TimeVal> expected = UnmodifiableList.of(
        TimeVal.from(0, 10.0),
        TimeVal.from(10, 10.0),
        TimeVal.from(30, 10.0));
    assertMetrics(10, 20, inputList, expected);
  }

  long t(int m, int s) {
    return (m * 60 + s) * 1000L;
  }

  @Test
  public void testRandomOffset() throws Exception {
    List<Metric> inputList = UnmodifiableList.of(
        newMetric(t(1, 13), 1.0),
        newMetric(t(2, 13), 1.0),
        newMetric(t(3, 13), 1.0));

    List<TimeVal> expected = UnmodifiableList.of(
        TimeVal.from(t(1, 0), 47.0 / 60.0),
        TimeVal.from(t(2, 0), 1.0),
        TimeVal.from(t(3, 0), 1.0));

    assertMetrics(60000, 120000, inputList, expected);
  }

  private List<Metric> getValue(List<? extends AbstractMonitor<Number>> monitors, Clock clock) {
    List<Metric> result = new ArrayList<>();
    for (AbstractMonitor<Number> m : monitors) {
      Number n = m.getValue(0);
      Metric metric = new Metric(m.getConfig(), clock.now(), n);
      result.add(metric);
    }
    return result;
  }

  private static final double DELTA = 1e-6;

  @Test
  public void testUpdate() throws Exception {
    BasicCounter basicCounter = new BasicCounter(MonitorConfig.builder("basicCounter").build());
    ManualClock manualClock = new ManualClock(0);
    StepCounter stepCounter = new StepCounter(MonitorConfig.builder("stepCounter").build(),
        manualClock);
    LongGauge gauge = new LongGauge(MonitorConfig.builder("longGauge").build());

    List<? extends AbstractMonitor<Number>> monitors = UnmodifiableList.of(basicCounter,
        stepCounter, gauge);

    MemoryMetricObserver observer = new MemoryMetricObserver("normalization-test", 1);
    NormalizationTransform normalizationTransform = new NormalizationTransform(observer, 60,
        120, TimeUnit.SECONDS, manualClock);
    CounterToRateMetricTransform toRateMetricTransform =
        new CounterToRateMetricTransform(normalizationTransform, 60,
            120, TimeUnit.SECONDS, manualClock);

    double[] rates = {0.5 / 60.0, 2 / 60.0, 3 / 60.0, 4 / 60.0};
    double[] expectedNormalized = {
        rates[0] * (2.0 / 3.0), // 20000L over stepBoundary
        rates[0] * (1.0 / 3.0) + rates[1] * (2.0 / 3.0),
        rates[1] * (1.0 / 3.0) + rates[2] * (2.0 / 3.0),
        rates[2] * (1.0 / 3.0) + rates[3] * (2.0 / 3.0)};

    for (int i = 1; i < 5; ++i) {
      long now = 20000L + i * 60000L;
      long stepBoundary = i * 60000L;
      manualClock.set(now);
      basicCounter.increment(i);
      stepCounter.increment(i);
      gauge.set((long) i);
      List<Metric> metrics = getValue(monitors, manualClock);
      toRateMetricTransform.update(metrics);

      List<Metric> o = observer.getObservations().get(0);
      assertEquals(o.size(), 3);
      double basicCounterVal = o.get(0).getNumberValue().doubleValue();
      double stepCounterVal = o.get(1).getNumberValue().doubleValue();
      double gaugeVal = o.get(2).getNumberValue().doubleValue();
      assertEquals(gaugeVal, (double) i, DELTA);
      // rate per second for the prev interval
      assertEquals(stepCounterVal, (i - 1) / 60.0, DELTA);
      assertEquals(basicCounterVal, expectedNormalized[i - 1], DELTA);

      for (Metric m : o) {
        assertEquals(m.getTimestamp(), stepBoundary);
      }
    }

    // no updates to anything, just clock forward
    int i = 5;
    manualClock.set(i * 60000L + 20000L);
    List<Metric> metrics = getValue(monitors, manualClock);
    toRateMetricTransform.update(metrics);
    List<Metric> o = observer.getObservations().get(0);
    assertEquals(o.size(), 3);

    double basicCounterVal = o.get(0).getNumberValue().doubleValue();
    double stepCounterVal = o.get(1).getNumberValue().doubleValue();
    double gaugeVal = o.get(2).getNumberValue().doubleValue();

    assertEquals(gaugeVal, (double) 4, DELTA); // last set value
    assertEquals(stepCounterVal, 4 / 60.0, DELTA);
    assertEquals(basicCounterVal, (1 / 3.0) * rates[3]);
  }

  @Test
  public void testExpiration() {
    BasicCounter c1 = new BasicCounter(MonitorConfig.builder("c1").build());
    BasicCounter c2 = new BasicCounter(MonitorConfig.builder("c2").build());
    BasicCounter c3 = new BasicCounter(MonitorConfig.builder("c3").build());
    ManualClock manualClock = new ManualClock(0);

    MemoryMetricObserver observer = new MemoryMetricObserver("normalization-test", 1);
    NormalizationTransform normalizationTransform = new NormalizationTransform(observer, 60,
        120, TimeUnit.SECONDS, manualClock);
    CounterToRateMetricTransform toRateMetricTransform =
        new CounterToRateMetricTransform(normalizationTransform, 60,
            120, TimeUnit.SECONDS, manualClock);

    manualClock.set(30000L);
    c1.increment();
    Metric m1 = new Metric(c1.getConfig(), manualClock.now(), c1.getValue(0));

    toRateMetricTransform.update(UnmodifiableList.of(m1));
    assertEquals(NormalizationTransform.HEARTBEAT_EXCEEDED.getValue(0).longValue(), 0);
    List<Metric> o = observer.getObservations().get(0);
    assertEquals(o.size(), 1);

    manualClock.set(100000L);
    Metric m2 = new Metric(c2.getConfig(), manualClock.now(), c2.getValue());
    toRateMetricTransform.update(UnmodifiableList.of(m2));
    assertEquals(NormalizationTransform.HEARTBEAT_EXCEEDED.getValue(0).longValue(), 0);

    manualClock.set(160000L);
    Metric m3 = new Metric(c3.getConfig(), manualClock.now(), c3.getValue());
    toRateMetricTransform.update(UnmodifiableList.of(m3));
    assertEquals(NormalizationTransform.HEARTBEAT_EXCEEDED.getValue(0).longValue(), 1);

  }
}

package com.netflix.servo.monitor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class MaxGaugeTest extends AbstractMonitorTest<MaxGauge> {
    @Override
    public MaxGauge newInstance(String name) {
        return new MaxGauge(MonitorConfig.builder(name).build());
    }

    @Test
    public void testUpdate() throws Exception {
        MaxGauge maxGauge = newInstance("max1");
        maxGauge.update(42L);
        assertEquals(maxGauge.getValue().longValue(), 42L);
        maxGauge.update(420L);
        assertEquals(maxGauge.getValue().longValue(), 420L);
        maxGauge.update(1L);
        assertEquals(maxGauge.getValue().longValue(), 420L);
    }
}

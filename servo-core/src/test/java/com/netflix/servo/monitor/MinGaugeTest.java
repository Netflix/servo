package com.netflix.servo.monitor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class MinGaugeTest extends AbstractMonitorTest<MinGauge> {
    @Override
    public MinGauge newInstance(String name) {
        MonitorConfig config = MonitorConfig.builder(name).build();
        return new MinGauge(config);
    }

    @Test
    public void testUpdate() throws Exception {
        MinGauge minGauge = newInstance("min1");
        minGauge.update(42L);
        assertEquals(minGauge.getValue().longValue(), 42L);
        minGauge.update(420L);
        assertEquals(minGauge.getValue().longValue(), 42L);
        minGauge.update(1L);
        assertEquals(minGauge.getValue().longValue(), 1L);
    }
}

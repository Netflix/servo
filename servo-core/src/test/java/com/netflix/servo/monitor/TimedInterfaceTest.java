package com.netflix.servo.monitor;

import com.netflix.servo.DefaultMonitorRegistry;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class TimedInterfaceTest  {
    /**
     * Dummy interface to test our timer
     */
    private static interface IDummy {
        void method1();
        boolean method2(int n);
        Object method3(Object a, Object b);
    }

    private static class DummyImpl implements IDummy {
        private void sleep(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public void method1() {
            sleep(5);
        }

        @Override
        public boolean method2(int n) {
            sleep(15);
            return n > 0;
        }

        @Override
        public Object method3(Object a, Object b) {
            sleep(30);
            return a;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTimedInterface() {
        final IDummy dummy = TimedInterface.newProxy(IDummy.class, new DummyImpl(), "id");

        // you'd register the CompositeMonitor as:
        DefaultMonitorRegistry.getInstance().register((CompositeMonitor)dummy);

        for (int i = 0; i < 42; i++) {
            dummy.method1();
            if (i % 2 == 0) {
                dummy.method2(i);
            }
        }

        final CompositeMonitor<Long> compositeMonitor = (CompositeMonitor<Long>) dummy;
        final List<Monitor<?>> monitors = compositeMonitor.getMonitors();
        assertEquals(monitors.size(), 2);
        assertEquals(compositeMonitor.getValue().longValue(), 2L);
        final MonitorConfig expectedConfig = new MonitorConfig.Builder("IDummy")
                .withTag("class", "DummyImpl")
                .withTag("id", "id")
                .build();
        assertEquals(compositeMonitor.getConfig(), expectedConfig);

        for (Monitor<?> monitor: monitors) {
            final String method = monitor.getConfig().getTags().getTag("method").getValue();
            if (method.equals("method1")) {
                assertEquals(((Monitor<Long>)monitor).getValue().longValue(), 5);
            } else {
                assertEquals(method, "method2");
                assertEquals(((Monitor<Long>)monitor).getValue().longValue(), 15);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTimedInterfaceNoId() {
        final IDummy dummy = TimedInterface.newProxy(IDummy.class, new DummyImpl());

        // you'd register the CompositeMonitor as:
        DefaultMonitorRegistry.getInstance().register((CompositeMonitor)dummy);

        final CompositeMonitor<Long> compositeMonitor = (CompositeMonitor<Long>) dummy;
        final MonitorConfig expectedConfig = new MonitorConfig.Builder("IDummy")
                .withTag("class", "DummyImpl")
                .build();
        assertEquals(compositeMonitor.getConfig(), expectedConfig);
    }

}

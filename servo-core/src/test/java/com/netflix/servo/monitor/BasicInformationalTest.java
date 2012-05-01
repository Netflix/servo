package com.netflix.servo.monitor;

import com.netflix.servo.MonitorContext;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * User: gorzell
 * Date: 5/1/12
 */
public class BasicInformationalTest {
    @Test
    public void testGetValue() throws Exception {
        String testInfo = "Test Info String";
        BasicInformational testInformational = new BasicInformational(new MonitorContext.Builder("test").build());
        assertEquals(testInformational.getValue(), "");

        testInformational.setValue(testInfo);
        assertEquals(testInformational.getValue(), testInfo);
    }
}

package com.netflix.servo.publish.graphite;

import com.netflix.servo.Metric;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class GraphiteMetricObserverTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadAddress1() throws Exception
    {
        new GraphiteMetricObserver( "serverA", "127.0.0.1" );
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadAddress2() throws Exception
    {
        new GraphiteMetricObserver( "serverA", "http://google.com" );
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testBadAddress3() throws Exception
    {
        new GraphiteMetricObserver( "serverA", "socket://127.0.0.1:808" );
    }

    @Test
    public void testSuccessfulSend() throws Exception
    {
        SocketReceiverTester receiver = new SocketReceiverTester( 8082 );
        receiver.start();

        GraphiteMetricObserver gw = new GraphiteMetricObserver( "serverA", "127.0.0.1:8082" );

        try
        {
            List<Metric> metrics = new ArrayList<Metric>();
            metrics.add(BasicGraphiteNamingConventionTest.getOSMetric("AvailableProcessors"));

            gw.update(metrics);

            receiver.waitForConnected();

            String[] lines = receiver.waitForLines( 1 );
            assertEquals( 1, lines.length );

            assertEquals(lines[0].indexOf("serverA.java.lang.OperatingSystem.AvailableProcessors"), 0);

        }
        finally
        {
            receiver.stop();
            gw.stop();
        }
    }
}

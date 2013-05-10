/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netflix.servo.monitor;

import static com.netflix.servo.monitor.PeakRateCounterTest.SAMPL_INTERVAL;
import com.netflix.servo.tag.Tag;
import static java.lang.Thread.sleep;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 *
 * @author kelleystover
 */
public class PeakRateCounterTest extends AbstractMonitorTest<PeakRateCounter> {

    /* testing Note:
     * the value of SAMPL_INTERVAL will cause the threading test to run for atleast that long
     * so if it starts causing unit tests to run too long, try shortening it.
     * long running threading tests probably belong somewhere else since they
     * slow down test driven approach, not in these
     * unit tests (where to put them?)
     */
    static final long SAMPL_INTERVAL = 30;
    static final TimeUnit UNIT = TimeUnit.SECONDS;
    static final long SAMPL_INTERVAL_MILLIS = TimeUnit.MILLISECONDS.convert(SAMPL_INTERVAL, UNIT);

    @Override
    public PeakRateCounter newInstance(String name) {
        return new PeakRateCounter(MonitorConfig.builder(name).build(), SAMPL_INTERVAL, UNIT);

    }


    /*
     * Tests logic correctness without interval timer thread running
     * 
     * Mimics similar test cases to CounterToRateMetricTransformTest.testSimpleRate
     * as a way to give the reader a quick way to understand the difference in the values
     * produced by the PeakRateCounter
     */
    @Test
    public void testSimpleRate() throws Exception {
        PeakRateCounter c = newInstance("foo");


        long baseTime = System.currentTimeMillis();
        long ellapsedTime = baseTime;

        //as if client code had started the rate counter
        c.takeSnapshot(baseTime);

        long totalCount = c.getTotalCount();
        assertEquals(totalCount, 0);
        long peakCount = c.getValue();
        assertEquals(peakCount, 0);
        double peakRate = c.getPeakRate();
        assertEquals(peakRate, 0.0);

        // Delta of 5 in 5 seconds

        c.increment(5); //as if client code had incremented the counter

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 5);
        peakCount = c.getValue();
        assertEquals(peakCount, 0);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 0.0);


        ellapsedTime = baseTime + 5000;

        c.updatePeakRate(ellapsedTime); //as if the interval timer had gone off

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 5);
        peakCount = c.getValue();
        assertEquals(peakCount, 1);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 1.0);


        // Delta of 15 in 5 seconds

        c.increment(15);
        ellapsedTime += 5000;
        c.updatePeakRate(ellapsedTime);

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 20);
        peakCount = c.getValue();
        assertEquals(peakCount, 1);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 3.0);

        // No change from previous sample - e.g. no new samples
        //
        //the average rate for this interval will be 0.0 because there
        //are no new samples to compare against the snapshot
        //but the counter will leave the peakRate and PeakRateCounter unchanged
        //note this is different result expected than if the counter value was
        //transformed by the CounterToRateMetricTransform because it is producing
        //the average rate instead of the peak rate
        ellapsedTime += 5000;
        c.updatePeakRate(ellapsedTime);

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 20);
        peakCount = c.getValue();
        assertEquals(peakCount, 1);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 3.0);


        //
        // Same peak rate reached as previous, expect peak rate counter to increase
        // delta of 15 in 5 seconds
        c.increment(15);
        ellapsedTime += 5000;
        c.updatePeakRate(ellapsedTime);

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 35);
        peakCount = c.getValue();
        assertEquals(peakCount, 2);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 3.0);

        //
        // New Peak Rate, previously unreached, 
        // expect peak rate counter value to reset to 1 since is first time seen
        // 30 in 5 seconds

        c.increment(30);
        ellapsedTime += 5000;
        c.updatePeakRate(ellapsedTime);

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 65);
        peakCount = c.getValue();
        assertEquals(peakCount, 1);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 6.0);



        // 
        // Decrease from previous sample
        // this use case would happen when the counter is reset
        //  !ks todo - check whether counter will wrap past size of long
        // is a valid use case - do other counters or CounterToRateMetricTransform
        // handle this case?
        //

        //capture the time of reset
        ellapsedTime = System.currentTimeMillis();
        long lastPeakCount = c.getAndResetValue();
        //long lastPeakCount = c.getValueAndPauseCounter();

        assertEquals(lastPeakCount, 1);

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 0);
        peakCount = c.getValue();
        assertEquals(peakCount, 0);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 0.0);

        //
        // Still ok after reset, but now old Peak Rate is first time not 2nd
        // expect peak rate counter value to increment to 1 since is first time seen
        // 30 in 5 seconds after reset

        c.increment(30);    //does not adjust the peakRate or peakRateCount
        totalCount = c.getTotalCount();
        assertEquals(totalCount, 30);
        peakCount = c.getValue();
        assertEquals(peakCount, 0);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 0.0);

        ellapsedTime += 5000;
        c.updatePeakRate(ellapsedTime);

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 30);
        peakCount = c.getValue();
        assertEquals(peakCount, 1);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 6.0);

        //test for expired snapshot
        //expect the computed rate to drop to 0 because we drop off the old snapshot
        //and wait for a new sample (counter increment) within the sampled time.
        //peakRateCount will be unchanged
        //peakRate will be unchanged as we have not seen a new high value
        //
        ellapsedTime += SAMPL_INTERVAL_MILLIS + 1;

        c.updatePeakRate(ellapsedTime);

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 30);
        peakCount = c.getValue();
        assertEquals(peakCount, 1);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 6.0);


        //after a sample was dropped,
        // add in a new sample and the computed rate should become 10.0
        //with peakRate becomes 10.0 and peakRateCount = 1 since is a new high
        //
        c.increment(50);
        ellapsedTime += 5000;

        c.updatePeakRate(ellapsedTime);

        totalCount = c.getTotalCount();
        assertEquals(totalCount, 80);
        peakCount = c.getValue();
        assertEquals(peakCount, 1);
        peakRate = c.getPeakRate();
        assertEquals(peakRate, 10.0);

    }

    // test with sampling interval thread running
    class ThreadingTester extends Thread {

        final PeakRateCounter counter;
        final long testDelay;
        final long testIncrement;
        final long expectedTotalCount;
        final long expectedPeakCount;
        final double expectedPeakRate;

        ThreadingTester(PeakRateCounter c, long delay, long increment, long totalCount, long peakCount, double peakRate) {
            counter = c;
            testDelay = delay;
            testIncrement = increment;
            expectedTotalCount = totalCount;
            expectedPeakCount = peakCount;
            expectedPeakRate = peakRate;
        }

        @Override
        public void run() {
            try {
               
                
                counter.increment(testIncrement);
                sleep(testDelay * 2); //wait a few more intervals
                
                long totalCount = counter.getTotalCount();
                assertEquals(totalCount, expectedTotalCount);
                long dropped = counter.getDroppedSampleCount();
                assertEquals(dropped, 0);
                long peakCount = counter.getValue();
                assertEquals(peakCount, expectedPeakCount);
                double peakRate = counter.getPeakRate();
              //fudge exact peakRate within range - lets see if consistent.
                //not the best test but looks like maybe execution overhead
                //could be causing calculation of duration in milliseconds
                //based on system time in milliseconds to vary a bit?
                //so should we measure the time in finer grain & would the values
                //actually be finer grained? (system gets millis)
                //or a programming error causing some rounding?
                double lowerExpected = expectedPeakRate - 0.001;
                double upperExpected = expectedPeakRate + 0.001;
                assertTrue( (lowerExpected <= peakRate) && (peakRate <= upperExpected), 
                        "peakRate is " + peakRate
                        + ", lowerExpected = " + lowerExpected
                        + ", upperExpected = " + upperExpected);               
                
                
            } catch (InterruptedException ex) {
                Logger.getLogger(PeakRateCounterTest.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                //do we need ? counter.stop() that would tidy up the interval thread ?;
                //in the test the counter will be garbage collected
                //!ks todo figure out how to confirm when the counter's thread will
                //actually be cleaned up by the garbage collector
            }
        }
    }

    /*
     * !ks todo chg to drive this with test data from a file
     * see 10 increments within 10 seconds
     */
    @Test
    public void testThreadedRate() throws Exception {
        long delay = SAMPL_INTERVAL_MILLIS;
        long increment = 30L;
        long expectedTotalCount = 30L;
        long expectedPeakCount = 1L;
        double expectedPeakRate = 1.0;

        PeakRateCounter c = newInstance("bar");
        c.start();
        PeakRateCounterTest.ThreadingTester t = new PeakRateCounterTest.ThreadingTester(c, delay, increment, expectedTotalCount, expectedPeakCount, expectedPeakRate);
        t.run();  
        double peak = c.getPeakRate();
    }

    @Test
    public void testHasGaugeTag() throws Exception {
        Tag type = newInstance("foo").getConfig().getTags().getTag("type");
        assertEquals(type.getValue(), "GAUGE");
    }

    @Test
    public void testEqualsCount() throws Exception {
        PeakRateCounter c1 = newInstance("foo");
        PeakRateCounter c2 = newInstance("foo");
        assertEquals(c1, c2);

        c1.increment();
        assertNotEquals(c1, c2);
        c2.increment();
        assertEquals(c1, c2);
    }

    @Test
    public void testEqualsAndHashCodeName() throws Exception {
        PeakRateCounter c1 = newInstance("1234567890");
        PeakRateCounter c2 = newInstance("1234567890");
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        c2 = c1;
        assert (c2 == c1);
        assertEquals(c2, c1);
    }
}


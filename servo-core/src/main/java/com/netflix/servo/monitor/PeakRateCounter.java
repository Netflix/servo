/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.netflix.servo.monitor;

import com.google.common.base.Objects;
import com.netflix.servo.annotations.DataSourceType;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author kelleystover
 */
public class PeakRateCounter extends AbstractMonitor<Long>
        implements Counter, ResettableMonitor<Long> {

    /* !ks todo - notes for refactoring to thread safe:
     * these variables should  be updated  atomic transactions so figure out how to sync that behavior 
     * and still allow for multiple counter clients to get the counter value (read)
     * without having to wait for eachother.  
     * Allow clients to increment the counter with safe interaction
     * Ensure the timer thread can come and get the value and update the rate
     * without blocking the client that needs to increment the timer
     * and without drifting from the interval time heartbeat.
     * 
     */
    private final AtomicLong totalCount = new AtomicLong(0L);      //running total all increments
    private Double peakRate;       //the highest value of an average rate per second since the counter was reset
    private final AtomicLong peakRateCount = new AtomicLong(0L);  // count of the number of times the peakRate was reached since last reset
    private final PeakRateCounter.ValueSnapshot snapShot = new PeakRateCounter.ValueSnapshot();
    private final Thread intervalTimer;
    private long samplingInterval = 10;
    private TimeUnit samplingIntervalUnit = TimeUnit.SECONDS;
    private long samplingIntervalMillis;
    private final AtomicLong droppedSampleCount = new AtomicLong(0L);

    /**
     * Create a new instance of the counter. Reports the peak rate per second of
     * the counter. The raw measurement is taken in milli seconds which is the
     * granularity of the system for example a PeakRateCounter with a total
     * totalCount of 15 and rate rateUnit = SECONDS is reset after 5000
     * milliseconds. If the totalCount sample at the end of each of the five
     * 1-second intervals was as follows: {1, 5, 0, 7, 2 } The average rate
     * would be reported as 3.0 counts per second The peak rate would be
     * reported as 7.0 counts per second
     *
     * To change the units of the rate, e.g. to report it in other Time Units,
     * Re-factor to pass the time unit around and consider limiting the smallest
     * sample granularity to milliseconds
     */
    public PeakRateCounter(MonitorConfig config, long samplingInterval, TimeUnit samplingIntervalUnit) {
        // This class will reset the value so it is not a monotonically increasing value as
        // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
        // the publishing pipeline receiving the value.
        // this technique copied from ResettableCounter 
        //!ks todo - consider refactor into subclass of ResettableCounter
        //when looking at class/interface hierarchy 
        super(config.withAdditionalTag(DataSourceType.GAUGE));
        this.samplingInterval = samplingInterval;
        this.samplingIntervalUnit = samplingIntervalUnit;
        this.samplingIntervalMillis = TimeUnit.MILLISECONDS.convert(samplingInterval, samplingIntervalUnit);
        this.peakRate = 0.0;

        intervalTimer = new Thread(new PeakRateCounter.PeakRateComputer(samplingIntervalMillis));
    }

    public void start() {
        intervalTimer.start();
    }

    public Double getPeakRate() {  //add an interface for RateCounter or RateMonitor
        return peakRate;
    }

    public Long getPeakRateAsLong() {  //in case client wants to get and xform themselves
        //add an interface for RateCounter or RateMonitor
        //examine this when refactoring to thread safe and see if AtomicLong can store the double as bits
        //and if that makes any sense.  probably better idea to clean up the class/interface contract to include
        //new notion of a rate counter
        //
        return peakRate.longValue();
    }

    public Long getPeakRateCount() {  
        return peakRateCount.get();
    }

    public Long getDroppedSampleCount() {  //this is for debugging.  should the counter publish it's own metrics?
        return droppedSampleCount.get();
    }

    public Long getTotalCount() {  //this is the value a non-Rate counter would return from getValue()
        return totalCount.get();
    }

    @Override
    public Long getValue() {            //this is confusing for users -this counter interface is for the underlying value that gets incremented in other counters
        return getPeakRateCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment() {
        totalCount.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void increment(long amount) {
        totalCount.getAndAdd(amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getAndResetValue() {  //!ks confusing interface for users because increment increments the total value and this returns the peak rate count
        intervalTimer.interrupt();
         Long value = getAndResetCounter();
        intervalTimer.start();
        return value;
    }
    Long getAndResetCounter() {
        totalCount.getAndSet(0L);
        peakRate = 0.0;
        Long value = peakRateCount.getAndSet(0L);
        droppedSampleCount.set(0L);
        takeSnapshot(System.currentTimeMillis());
        return value;
    }

   

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PeakRateCounter)) {
            return false;
        }
        PeakRateCounter c = (PeakRateCounter) obj;
        return config.equals(c.getConfig())
                && totalCount.get() == c.totalCount.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(config, totalCount.get());
    }
    /* resettable counter hashcode
     * * Why doesn'intervalTimer the Basic Counter include the totalCount in it's hash?
     * The basic test using newly constructed objects testing against two with different names
     * succeeds because the initial value of the counter is zero, but for two counters that have been
     * in use, it will fail if the totalCount is different for a resettable counter and will still be successful
     * for a basic counter which seems inconsistent.
     * 
     * the default abstract monitor test for hashcode using just the names seems wrong
     * for counters since it takes advantage of this initialized state
     * 
     * !ToDo Should equals and hashcode rely on the countTimeStamp for rate counter?
     * 

     public int hashCode() {
     return Objects.hashCode(config, totalCount.get());
     }
     * */

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("totalCount", totalCount.get())
                .add("peakRateCount", peakRateCount.get())
                .add("peakRate", peakRate)
                .add("samplingInterval", samplingInterval)
                .add("samplingIntervalUnit", samplingIntervalUnit)
                .toString();
    }

    void takeSnapshot(long timestamp) {
        snapShot.update(totalCount.get(), timestamp);
    }

    void updatePeakRate(long now) {
        // check for expired old snapshot
        long ellapsedTime = now - snapShot.timestamp;
        if (ellapsedTime > samplingIntervalMillis * 2) {

            snapShot.update(totalCount.get(), now);
            droppedSampleCount.incrementAndGet();
            //rate = 0 so don'intervalTimer increase the peak count

        } else {
            double rate = snapShot.computeAverageRatePerSecond(totalCount.get(), now);
            if (rate > peakRate) {
                peakRate = rate;
                peakRateCount.set(1);
            } else {
                if (rate == peakRate) {
                    long cnt = peakRateCount.incrementAndGet();
                }
            }
        }
    }

    
    class ValueSnapshot {

        private long value = 0;
        private long timestamp = 0;

        void update(long value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;

        }

        /*
         * why do these local variables need to be final? 
         * they are on the stack of the thread and can not be clobbered by another thread.
         * but the thread executing the series of instructions could be
         * interrupted so that although
         //final makes each one immutable the set of variables is potentially
         //inconsistent.  and the timestamp and value variables are shared but not guarded
         //  setting timestamp and value individually is also not atomic
         //
         // copied this code frm CounterToRateMetricTransform which states it is not threadsafe 
         // and needs to be wrapped.  
         // !ks go look at wrapper.
         */
        public double computeAverageRatePerSecond(long curValue, long curTimestamp) {

            final long currentValue = curValue;
            final long currentTimestamp = curTimestamp;

            final double millisPerSecond = 1000.0;
            final double duration = (currentTimestamp - timestamp) / millisPerSecond;

            final double delta = currentValue - value;

            value = currentValue;
            timestamp = currentTimestamp;

            return (duration <= 0.0 || delta <= 0.0) ? 0.0 : delta / duration;

        }
        
    }

    class PeakRateComputer implements Runnable {

        private long duration = 0L;

        public PeakRateComputer(long duration) {

            this.duration = duration;
        }

        @Override
        public void run() {
            try {
                takeSnapshot(System.currentTimeMillis());
                //since this loop sleeps, while(true) is probably ok
                //sleep should respond to a call to .interrupt() and 
                //checking if current thread is interrupted may not matter
                //!ks todo confirm
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(duration);
                    updatePeakRate(System.currentTimeMillis());
                }
            } catch (InterruptedException e) {
                getAndResetCounter();
            }
        }
    }
}

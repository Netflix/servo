[![Build Status](https://netflixoss.ci.cloudbees.com/job/servo-master/badge/icon)](https://netflixoss.ci.cloudbees.com/job/servo-master/)

Generating Metrics with Servo
=============================

Metrics and Monitors
--------------------

A `Monitor` in servo has a configuration (`MonitorConfig`) and a way to get its
current value. The configuration is just a set of key=value pairs that uniquely
identify the monitor.  There is only one required key: `name`

A metric represents the value a given `Monitor` (identified by its
`MonitorConfig`) returned at a particular point in time.

Splitting the attributes in key=value pairs allows for much greater query time
flexibility. A backend system that collects metrics reported by many nodes can
compute aggregations for any key/value combination. If all the interesting
attributes for a metric were encoded in the name then it would be required to
create and maintain fragile regular expressions (and in many cases the desired
aggregation would be impossible to achieve.)

Monitor Types
-------------

### Basic types

* Gauges
* Counters
* Timers

### Gauges

A `Gauge` is simply a `Monitor` that returns a current value. For example the
one minute load average, the size of a queue, the number of threads in a
thread pool, etc. 

Servo provides several implementations of gauges:

#### BasicGauge

A `BasicGauge` invokes a given `Callable` to get the current value. For example:

```java
 ...
 private final BlockingQueue<UpdateTasks> pushQueue;

 private final Gauge<Integer> pushQueueSize = new BasicGauge<Integer>(
            MonitorConfig.builder("pushQueue").build(), new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            return pushQueue.size();
        }
    });
```

#### MinGauge and MaxGauge

A `MinGauge` is a gauge that keeps track of the minimum value seen since the
last reset. A `MaxGauge` is a gauge that keeps track of the maximum value seen
since the last reset. Updates should be non-negative. In case of no updates the
value 0 is returned.

```java
  ...
  private final MinGauge minDiskFree = new MinGauge(MonitorConfig.builder("diskFreeMin").build());
  private final MaxGauge maxDiskFree = new MaxGauge(MonitorConfig.builder("diskFreeMax").build());

  ...
  private void updateDiskFreeStats(long curValue) {
    minDiskFree.update(curValue);
    maxDiskFree.update(curValue);
  }
```

#### NumberGauge: LongGauge and DoubleGauge

A `NumberGauge` just wraps a provided `Number`. The number needs to be be thread-safe for access by a background thread.   For example:

```java

  private final AtomicInteger n = new AtomicInteger(0);
  private final NumberGauge gauge = new NumberGauge(MonitorConfig.builder("someNumber").build(), n);
  ...

  private void update() {
    if (someCondition) {
        n.set(10);
    } else {
        n.incrementAndGet();
    }
  }
```

Two very common use cases are to wrap `AtomicLong` or `AtomicDouble` numbers,
and the provided `LongGauge` and `DoubleGauge` do just that:

```java
  private final LongGauge tp90 = new LongGauge(MonitorConfig.builder("tp90").build());
  private final DoubleGauge stdDev = new DoubleGauge(MonitorConfig.builder("stddev").build());
  ...

  private void computeStats() {
    // compute these
    long computedP90 = 10;
    double computedStdDev = 1.0;

    // then
    tp90.set(computedP90);
    stdDev.set(computedStdDev);
  }
```

### Counters

Counters are monitors that provide `increment()` and `increment(long
delta)` methods. There are several types of counters available in Servo. You
can get a default `Counter` by calling `Monitors.newCounter(String name)`, or
you can instantiate a specific type of `Counter`. For example:

```java

    Counter basic = new BasicCounter(MonitorConfig.builder("basic").build());
    Counter rate = new ResettableCounter(MonitorConfig.builder("resettable").build());
    Counter peakRate = new PeakRateCounter(MonitorConfig.builder("peak").build());

    void someMethod() {
        basic.increment();
        rate.increment();
        peakRate.increment();
    }
```

The difference among counter types is what they report on the getValue() call.

* `BasicCounter` returns a monotonically increasing value.
* `ResettableCounter` returns a rate per second for the given polling interval.
  (See Resettable Monitors and Polling Intervals)
* `PeakRateCounter` returns the maximum count for a given second during the
  polling interval.

At Netflix we send all counters to our backend as rates. That means we have to
convert the value provided by `BasicCounter`. This is done using the Servo
`CounterToRateMetricTransform` observer. In order to convert a `BasicCounter`
to a rate two polling samples are required. A `ResettableCounter` can send the
value to backend as a rate with just one polling sample.

```java

public class Server {
    // just use the default implementation
    private final Counter totalConnections = Monitors.newCounter("totalConnections");
    private final Counter bytesIn = Monitors.newCounter("bytesIn");
    private final Counter bytesOut = Monitors.newCounter("bytesOut")
    ...

    private void setupNewConnection() {
        totalConnections.increment();
        ...
    }

    private void send(byte[] data) {
        // send data
        ...
        bytesOut.increment(data.length);
    }
}
```

### Timers

Timers allow you to measure how long a particular event takes.

#### BasicTimer

A `BasicTimer` is a composite monitor that generates four metrics per polling
interval:

* The minimum value that has been recorded. `statistic=min`
* The maximum value that has been recorded. `statistic=max`
* The total time recorded. `statistic=totalTime`
* The number of times the event has been recorded. `statistic=count`

```java
public class Server {
    private final Timer doSomethingTimer = new BasicTimer(
        MonitorConfig.builder("doSomething").build());
    private final Timer doSomethingElseTimer = new BasicTimer(
        MonitorConfig.builder("doSomethingElse").build());

    public void doSomething() {
        Stopwatch s = doSomethingTimer.start();
        try {
            // do something
            ...
        } finally {
            s.stop();
        }
    }

    public void doSomethingElse() {
        long timeInMs = thisReturnsTheTimeInMsItTakes();
        doSomethingElseTimer.record(timeInMs, TimeUnit.MILLISECONDS);
    }
}
```

Assuming doSomething() was called twice and the recorded times were 5ms and 15ms
the following metrics will be generated:

* name=doSomething statistic=totalTime value=20
* name=doSomething statistic=count value=2
* name=doSomething statistic=min value=5
* name=doSomething statistic=max value=15

Note that by publishing totalTime and count it becomes possible to get accurate
averages across arbitrary aggregations later on.

#### BucketTimer

A timer that - in addition to min, max, count, totalTime - maintains a number of
"buckets". For example this can be used to keep track of how many times a call
was under (or equal to) 50ms, between 51-500ms, and above 500ms.

```java

public class Server {
    private final Timer bucketTimer = new BucketTimer(
        MonitorConfig.builder("getRecommendations").build(), 
        BucketConfig.builder()
            .withTimeUnit(TimeUnit.MILLISECONDS)
            .withBuckets(new long[] { 50, 500 })
            .build());

    public void getRecommendations() {
        Stopwatch s = bucketTimer.start();

        // call some external services
        ...

        s.stop();
    }
}

```

Assuming in the above code that the getRecommendations() method was called 7
times, and the values reported were: 49ms, 40ms, 400ms, 50ms, 500ms, 2000ms,
501ms. The following metrics would be available:

* name=getRecommendations statistic=totalTime (49 + 40 + 400 + 50 + 500 + 2000)
  value=3039
* name=getRecommendations statistic=min value=40
* name=getRecommendations statistic=max value=2000
* name=getRecommendations statistic=count servo.bucket=bucket=050ms value=3
* name=getRecommendations statistic=count servo.bucket=bucket=500ms value=2
* name=getRecommendations statistic=count servo.bucket=overflow value=1 

The main use of BucketTimer is as an alternative to percentiles that allows a
backend service to aggregate the values correctly.  For example in an AWS
deployment we might want to graph the distribution of values across buckets per
availability zone, or per autoScalingGroup, etc. To get the total count we'll
have to use statistic=count and ignore the servo.bucket tag, using an
aggregation function of sum.

#### StatsTimer

A `StatTimer` can collect different statistics for time measurements. For
example:

* min, max, count, totalTime (as in a BasicTimer)
* standard deviation and variance
* average
* arbitrary percentiles. For example: 10th, 50th, 99th, 99.5th

This is a much more expensive monitor, and it doesn't allow proper aggregation
by backend metrics systems. (For example if each machine will be reporting the 99th
percentile for a metric, there's no way to get an arbitrary 99th percentile across
multiple machines).

A critical configuration setting is the sample size, which should be set to
roughly the number of expected calls per polling interval (usually one minute)
for the timer.

```java

public class Server {
    private static StatsTimer newTimer(String name) {
        final double [] percentiles = {50.0, 95.0, 99.0, 99.5};
        final StatsConfig statsConfig = new StatsConfig.Builder()
                .withSampleSize(1000)
                .withPercentiles(percentiles)
                .withPublishStdDev(true)
                .build();
        final MonitorConfig config = MonitorConfig.builder(name).build();
        return new StatsTimer(config, statsConfig);
    }

    private final StatsTimer requestTimer = newTimer("requestTimer");
    
    void doRequest() {
        Stopwatch s = requestTimer.start();
        // do something
        ...
        s.stop();
    }
}
```

This will publish the following metrics:

* name=requestTimer statistic=count
* name=requestTimer statistic=totalTime
* name=requestTimer statistic=stdDev
* name=requestTimer statistic=percentile_50
* name=requestTimer statistic=percentile_95
* name=requestTimer statistic=percentile_99
* name=requestTimer statistic=percentile_99.50

Internally this timer uses a double buffering technique. It creates and reuses
two buffers (`StatsBuffer`) for the current and previous update intervals.
Updates record a sample in the 'current' buffer, and a background thread
computes the statistics from data on the 'previous' buffer handling the swapping
of current and previous in a thread safe manner. By default a single threaded
`ScheduledExecutorService` is used by all stat monitors, but you can pass your
own.

#### TimedInterface

This class creates a Proxy monitor that tracks all calls to methods of an interface.

```java

AmazonS3 client = TimedInterface.newProxy(AmazonS3.class,
        new AmazonS3Client(new InstanceProfileCredentialsProvider()));

```

Every time you call a method implemented by AmazonS3 the time the method takes will be tracked by a `BasicTimer`.

For example calling: 

```java

    client.listObjects("bucket");
    client.putObject("bucket", "some.key", new File("/some/file"));
    for (String key : ImmutableList.of("key-a", "key-b", "key-c")) {
        client.getObject("bucket", key);
    }

```

would create three BasicTimers: one with name="listObjects", one with
name="putObject", and one with name="getObject". Each will have a tag
interface=AmazonS3 and a tag class=AmazonS3Client. Additionally we could have
created the TimedInterface proxy using an extra id parameter to differentiate
among multiple instances of the timers. 

For registration purposes (see the next section) you can cast the 'client' to a
`CompositeMonitor`.

### CompositeMonitor

A `CompositeMonitor` is a special type of Monitor that is composed of a number
of sub-monitors. For example a BasicTimer is a CompositeMonitor that is composed
of:

* totalTime: A counter that keeps track of the total time spent
* count: A counter that keeps track of the number of times the 'record' method
  has been invoked (directly or indirectly through Stopwatch#stop() )
* min: A MinGauge to keep the minimum time spent
* max: A MaxGauge to keep that maximum time spent

This can be safely ignored if you're just using the existing monitors but it
becomes very useful when you want to create your own. In particular if you want
to return a variable set of Monitors at runtime this is how to do it.
DynamicCounter / DynamicTimer are implemented using this feature.

Registration of Metrics
-----------------------

In servo all monitors need to be registered to be reported.  One critically
important thing to remember is that they need to be registered exactly once.  Some types of monitors, such as DynamicCounters, will handle registration automatically.

The main ways to achieve this:

* Monitors are static members of the class and registered in a static
  initializer block.

```java

public final class AlertEvaluator {
    private static final Counter alertNotificationFailures = new ResettableCounter(
            MonitorConfig.builder("notificationFailures")
                .withTag("class", "AlertEvaluator")
                .build());
    static {
        DefaultMonitorRegistry.getInstance().register(alertNotificationFailures);
    }
    ...
```

Please note that we're manually adding a class tag. This is a convention that
lets us namespace metrics generated by different libraries.

* Monitors are members of a singleton and registered in the constructor.

```java

public enum SomeSingleton {
    INSTANCE;

    private final Counter someCount = Monitors.newCounter("someCount");

    SomeSingleton() {
        Monitors.registerObject(this);
    }
}
```

Note that the call to `Monitors.registerObject(this)` will use reflection to add
all instances of `Monitor`s that have been declared, and also add a tag class
with the value set to the class' simple name (class.getSimpleName()).

* Monitors are members of a class that will have very few instances that can be
  meaningfully distinguished.

```java

public class Server  {
    private final Timer t = Monitors.newTimer("someTimer");

    public Server(String vip) {
        Monitors.registerObject(vip, this);
    }

    ...
}
```

Note that in this case in addition to the class tag servo will also add an 'id'
tag to distinguish metrics among different instances.

* Use a helper class that will create or register a Monitor once.

```java
public class Servo {
    private static final ConcurrentMap<MonitorConfig, Counter> counters = 
        new ConcurrentHashMap<MonitorConfig, Counter>();
    private static final ConcurrentMap<MonitorConfig, Timer> timers = 
        new ConcurrentHashMap<MonitorConfig, Timer>();

    public static Counter getCounter(MonitorConfig config) {
        Counter v = counters.get(config);
        if (v != null) return v;
        else {
            Counter counter = new BasicCounter(config);
            Counter prevCounter = counters.putIfAbsent(config, counter)
                if (prevCounter != null) return prevCounter;
                else {
                    DefaultMonitorRegistry.getInstance().register(counter);
                    return counter;
                }
        }
    }

    public static Counter getCounter(Class c, String n) {
        MonitorConfig config = MonitorConfig.builder(n)
            .withTag("class", c.getSimpleName()).build();
        return getCounter(config); 
    }

    // similar methods for Timers
    ...
}

public class Server {
    final Counter errors = Servo.getCounter(Server.class, "fooNumErrors");
    final Timer t = Servo.getTimer(Server.class, "fooTimer");

    void doFoo() {
        Stopwatch s = t.start();
        try {
            // do something
            ...
            s.stop();
        } catch (Throwable t) {
            errors.increment();
            throw t;
        }
    }
}

```
* Use DynamicTimer / DynamicCounter. These helper classes will dynamically
  register a Monitor the first time it is used, and will also take care of
  expiring Monitors that been idle for a certain period of time (by default 15
  minutes).


```java

public class Server {
    void doStuff() {
        DynamicCounter.increment("numRequests", "class", "Server");
        try {
            ...
        } catch (Throwable t) {
            DynamicCounter.increment("numErrors", "class", "Server");
            throw t;
        }
    }

    void callSomeService() {
        Stopwatch s = DynamicTimer.start("someService", "class", "Server");
        try {
            // do the actual call here
            s.stop();
        } catch (Throwable t) {
            DynamicCounter.increment("someServiceErrors", "class", "Server",
                "error", t.getMessage());
            // or t.getClass().getSimpleName() 
            throw t;
        }

        // note that we don't call s.stop() since we don't want to record
        // the times for failed attempts
    }

    void callBar() {
        Stopwatch s = DynamicTimer.start("bar", "class", "Server");
        try {
            // do bar()
            s.stop();
        } catch (Throwable t) {
            // manually record the time it took for a particular exception
            // type
            DynamicTimer.record(MonitorConfig.builder("bar")
                .withTag("class", "Server")
                .withTag("error", t.getClass().getSimpleName())
                .build(), s.getDuration());
            throw t;
        }
    }
}
```

While these utility classes are very simple to use they do come with a
performance cost. It is much more expensive to invoke DynamicCounter.increment()
for example than simply counter.increment() (counter being a field in an
instance) since the former invokes an expensive map lookup operation every
time the counter is incremented. Using the helper class method is a more
performant way to achieve this, but extra care has to be taken to only create
needed monitors since they won't be expired after periods of inactivity.

Polling Intervals
-----------------

The previous section explained that all monitors need to be registered. Their
values are polled at regular intervals. These are called Polling Intervals. At
Netflix a common configuration is to have two polling intervals: 

* A slow polling interval that runs every 60 seconds

* A fast polling interval that runs every 10 seconds

Some policies determine the set of metrics (potentially aggregated using some
function like sum or count) that will be sent to our backend. The fast polling
interval will only send a very small number of metrics per machine (what we
consider critical metrics) and usually all the metrics collected during the slow
polling interval will be sent to a different set of backends.

For correctness some Monitors must be aware of the polling intervals. For
example a MaxGauge should report the maximum value that was recorded during a particular
interval. In the case of multiple pollers (a slow and a fast one)
normally each will get a different value.  The system property `servo.pollers` is used
for this purpose. It tells servo how many pollers will be used and their
frequency. For example: `-Dservo.pollers=60000,10000` tells servo that there
will be two pollers: poller at index 0 will run once a minute (60,000 ms) and
poller at index 1 will run every 10 seconds.


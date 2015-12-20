Provides an Observer that can be used to send metrics to [Atlas](https://github.com/Netflix/atlas)


```java

// The System Property servo.atlas.uri needs to point to your atlas deployment.
// For example -Dservo.atlas.uri=http://atlas.example.com/api/v1/publish

// These are tags that will be added to all metrics sent by the atlas observer.
TagList commonTags = BasicTagList.of("node", "i-1234", "cluster", "some.cluster");

// The actual atlas observer.
AtlasMetricObserver atlasObserver = new AtlasMetricObserver(new BasicAtlasConfig(), commonTags);

// Then at regular intervals
atlasObserver.update(metrics);
```

## Running Example

In one terminal, run Atlas:

```
$ curl -LO 'https://github.com/Netflix/atlas/releases/download/v1.4.4/atlas-1.4.4-standalone.jar'
$ curl -LO 'https://raw.githubusercontent.com/Netflix/atlas/v1.4.x/conf/memory.conf'
$ java -jar atlas-1.4.4-standalone.jar memory.conf
```

In another terminal, run the servo example app:

```
$ git clone git@github.com:Netflix/servo.git
$ cd servo
$ ./gradlew :servo-example:runWithAtlas
```

Make some requests to see activity on example counters:

```
$ echo "some content to post" > data
$ ab -c4 -n100 -p data 'http://localhost:12345/echo'
```

Then sample graphs can be viewed:

* [Threads by state](http://localhost:7101/api/v1/graph?q=name,threadCount,:eq,class,ThreadMXBean,:eq,:and,:max,(,state,),:by,$state,:legend,:stack&title=Threads%20by%20State&l=0&ylabel=count)
* [Memory usage](http://localhost:7101/api/v1/graph?q=name,actualUsage,:eq,class,MemoryPoolMXBean,:eq,:and,:avg,(,id,),:by,$(id),:legend&title=Memory+Usage)
* [Max request latency](http://localhost:7101/api/v1/graph?q=statistic,max,:eq,:max,(,class,),:by,name,latency,:eq,:cq&title=Max+Latency)
* [Bytes sent/received for echo](http://localhost:7101/api/v1/graph?q=name,(,bytesSent,bytesReceived,),:in,class,EchoHandler,:eq,:and,(,name,),:by,$name,:legend&title=Bytes+Sent+and+Received+for+EchoHandler)

## Step Size

Atlas stores data at a given step size, e.g. a datapoint per minute. This can be configured with
the `atlas.core.model.step` setting. See an example in
[memory.conf](https://github.com/Netflix/atlas/blob/master/conf/memory.conf#L6).

Servo should report datapoints at the same interval. The reporting interval can be configured
with the `server.pollers` system property which has a unit of milliseconds. See an example in
[build.gradle](https://github.com/Netflix/servo/blob/master/servo-example/build.gradle#L22)
for servo-example.

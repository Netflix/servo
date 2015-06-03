Provides an Observer that can be used to send metrics to [Atlas](https://github.com/Netflix/atlas)


```java

// The System Property servo.atlas.uri needs to point to your atlas deployment.
// For example -Dservo.atlas.uri=http://atlas.example.com/api/v1/publish-fast

// These are tags that will be added to all metrics sent by the atlas observer.
TagList commonTags = BasicTagList.of("node", "i-1234", "cluster", "some.cluster");

// The actual atlas observer.
AtlasMetricObserver atlasObserver = new AtlasMetricObserver(new BasicAtlasConfig(), commonTags);

// Then at regular intervals
atlasObserver.update(metrics);

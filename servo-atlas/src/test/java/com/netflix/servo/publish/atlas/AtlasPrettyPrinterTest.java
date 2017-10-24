package com.netflix.servo.publish.atlas;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.netflix.servo.Metric;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

import java.io.StringWriter;

import static org.testng.Assert.assertEquals;

public class AtlasPrettyPrinterTest {

  @Test
  public void testPayload() throws Exception {
    TagList commonTags = BasicTagList.of("nf.app", "example", "nf.cluster", "example-main", "nf.region", "us-west-3");
    Metric m1 = new Metric("foo1", BasicTagList.of("id", "ab"), 1000L, 1.0);
    Metric m2 = new Metric("foo2", BasicTagList.of("id", "bc", "class", "klz"), 1000L, 2.0);
    Metric m3 = new Metric("foo3", BasicTagList.EMPTY, 1000L, 3.0);
    Metric[] metrics = new Metric[] {m1, m2, m3};
    JsonPayload update = new UpdateRequest(commonTags, metrics, metrics.length);
    JsonFactory factory = new JsonFactory();
    StringWriter writer = new StringWriter();
    JsonGenerator generator = factory.createGenerator(writer);
    generator.setPrettyPrinter(new AtlasPrettyPrinter());
    update.toJson(generator);
    generator.close();
    writer.close();
    String expected = "{\n\"tags\":{\"nf.app\":\"example\",\"nf.cluster\":\"example-main\",\"nf.region\":\"us-west-3\"},\n\"metrics\":[\n" +
        "{\"tags\":{\"name\":\"foo1\",\"id\":\"ab\"},\"start\":1000,\"value\":1.0},\n" +
        "{\"tags\":{\"name\":\"foo2\",\"class\":\"klz\",\"id\":\"bc\"},\"start\":1000,\"value\":2.0},\n" +
        "{\"tags\":{\"name\":\"foo3\"},\"start\":1000,\"value\":3.0}]\n" +
        "}";

    assertEquals(writer.toString(), expected);
  }
}

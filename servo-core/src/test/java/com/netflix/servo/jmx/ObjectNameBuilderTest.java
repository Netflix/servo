package com.netflix.servo.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.testng.annotations.Test;

import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;

import static org.testng.Assert.*;

public class ObjectNameBuilderTest {

    @Test
    public void testInvalidCharactersSanitized() {
        ObjectName name =
                ObjectNameBuilder.forDomain("test*Domain&")
                                 .addProperty("foo%", "$bar")
                                 .build();
        assertEquals(name.getDomain(), "test_Domain_");
        assertEquals(name.getKeyPropertyListString(), "foo_=_bar");
    }

    @Test
    public void testAddTagList() {
        ObjectName name =
                ObjectNameBuilder.forDomain("testDomain")
                                 .addProperties(BasicTagList.of("foo", "bar", "test", "stuff"))
                                 .build();
        assertEquals(name.getDomain(), "testDomain");
        assertEquals(name.getKeyPropertyListString(), "test=stuff,foo=bar");
    }

    @Test
    public void testTagByTag() {
        // Order will be in the order tags were added to the builder
        ObjectName name =
                ObjectNameBuilder.forDomain("testDomain")
                                 .addProperty(new BasicTag("foo", "bar"))
                                 .addProperty(new BasicTag("test", "stuff"))
                                 .build();
        assertEquals(name.getDomain(), "testDomain");
        assertEquals(name.getKeyPropertyListString(), "foo=bar,test=stuff");
    }

    @Test
    public void testBuildWithoutPropertyAdded() {
        try {
            ObjectNameBuilder.forDomain("testDomain").build();
            fail("Should have thrown an exception without keys being added!");
        } catch (RuntimeException expected) {
            assertEquals(expected.getCause().getClass(), MalformedObjectNameException.class);
        }
    }

}

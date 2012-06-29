/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.jmx;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.netflix.servo.annotations.AnnotatedAttribute;
import com.netflix.servo.annotations.Monitor;

import javax.management.MBeanAttributeInfo;
import javax.management.openmbean.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

public final class MonitoredAttribute {

    private static final String TYPE_NAME = "MonitoredResourceMetadata";
    private static final String TYPE_DESC = "Metadata for monitored resource";

    private static final String[] ITEM_NAMES = {
        "Name", "Type", "Description", "Tags"
    };

    private static final OpenType<?>[] ITEM_TYPES;

    private static final CompositeType METADATA_TYPE;

    static {
        try {
            ITEM_TYPES = new OpenType<?>[] {
                SimpleType.STRING,                   // Name
                SimpleType.STRING,                   // Type
                SimpleType.STRING,                   // Description
                new ArrayType(1, SimpleType.STRING)  // Tags
            };

            METADATA_TYPE = new CompositeType(
                TYPE_NAME,
                TYPE_DESC,
                ITEM_NAMES,
                ITEM_NAMES,
                ITEM_TYPES);
        } catch (OpenDataException e) {
            // Should never happen unless there is a bug in the code
            throw new RuntimeException(e);
        }
    }

    private static final Map<Class<?>,SimpleType<?>> TYPES =
        ImmutableMap.<Class<?>,SimpleType<?>>builder()
            .put(BigDecimal.class, SimpleType.BIGDECIMAL)
            .put(BigInteger.class, SimpleType.BIGINTEGER)
            .put(Boolean.class,    SimpleType.BOOLEAN)
            .put(Boolean.TYPE,     SimpleType.BOOLEAN)
            .put(Byte.class,       SimpleType.BYTE)
            .put(Byte.TYPE,        SimpleType.BYTE)
            .put(Character.class,  SimpleType.CHARACTER)
            .put(Character.TYPE,   SimpleType.CHARACTER)
            .put(Date.class,       SimpleType.DATE)
            .put(Double.class,     SimpleType.DOUBLE)
            .put(Double.TYPE,      SimpleType.DOUBLE)
            .put(Float.class,      SimpleType.FLOAT)
            .put(Float.TYPE,       SimpleType.FLOAT)
            .put(Integer.class,    SimpleType.INTEGER)
            .put(Integer.TYPE,     SimpleType.INTEGER)
            .put(Long.class,       SimpleType.LONG)
            .put(Long.TYPE,        SimpleType.LONG)
            .put(Short.class,      SimpleType.SHORT)
            .put(Short.TYPE,       SimpleType.SHORT)
            .put(String.class,     SimpleType.STRING)
            .build();

    private final AnnotatedAttribute attr;

    private final MBeanAttributeInfo metadataAttributeInfo;
    private final MBeanAttributeInfo valueAttributeInfo;

    private final CompositeDataSupport metadata;

    public MonitoredAttribute(AnnotatedAttribute attr) {
        this.attr = Preconditions.checkNotNull(
            attr, "attribute cannot be null");

        Monitor anno = attr.getAnnotation();
        String name = anno.name();
        String type = anno.type().name();
        String desc = anno.description();

        metadataAttributeInfo = new OpenMBeanAttributeInfoSupport(
            name,
            "".equals(desc.trim()) ? name : desc,
            METADATA_TYPE,
            true,      // isReadable
            false,     // isWritable
            false);    // isIs

        valueAttributeInfo = new OpenMBeanAttributeInfoSupport(
            name,
            "".equals(desc.trim()) ? name : desc,
            getType(attr.getAttribute()),
            true,      // isReadable
            false,     // isWritable
            false);    // isIs

        try {
            metadata = new CompositeDataSupport(
                METADATA_TYPE,
                ITEM_NAMES,
                new Object[] {name, type, desc, attr.getTagsArray()});
        } catch (OpenDataException e) {
            throw new IllegalArgumentException(
                "failed to create mbean metadata value for " + toString(), e);
        }
    }

    public Monitor getAnnotation() {
        return attr.getAnnotation();
    }

    public Object getValue() throws Exception {
        return attr.getValue();
    }

    public Number getNumber() throws Exception {
        return attr.getNumber();
    }

    public CompositeDataSupport getMetadata() {
        return metadata;
    }

    public MBeanAttributeInfo getMetadataAttributeInfo() {
        return metadataAttributeInfo;
    }

    public MBeanAttributeInfo getValueAttributeInfo() {
        return valueAttributeInfo;
    }

    private OpenType<?> getType(AccessibleObject obj) {
        SimpleType<?> t = null;
        if (obj instanceof Field) {
            Field f = (Field) obj;
            t = TYPES.get(f.getType());
        } else {
            Method m = (Method) obj;
            t = TYPES.get(m.getReturnType());
        }
        return (t == null) ? SimpleType.STRING : t;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("attr", attr)
            .toString();
    }
}

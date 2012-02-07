/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
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
package com.netflix.servo.annotations;

import com.google.common.collect.ImmutableList;

import com.netflix.servo.BasicTagList;
import com.netflix.servo.Tag;
import com.netflix.servo.TagList;

import com.netflix.servo.jmx.MonitoredAttribute;

import java.lang.annotation.Annotation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.List;

/**
 * Helper functions for querying the monitor annotations associated with a
 * class.
 */
public final class AnnotatedAttribute {

    private final Object obj;
    private final Monitor anno;
    private final AccessibleObject attr;
    private final TagList tags;
    private final String[] tagsArray;

    public AnnotatedAttribute(Object obj, Monitor anno, AccessibleObject attr) {
        this(obj, anno, attr, BasicTagList.copyOf(anno.tags()));
    }

    public AnnotatedAttribute(
            Object obj,
            Monitor anno,
            AccessibleObject attr,
            TagList tags) {
        this.obj = obj;
        this.anno = anno;
        this.attr = attr;
        this.tags = tags;
        if (!attr.isAccessible()) {
            attr.setAccessible(true);
        }

        this.tagsArray = new String[tags.size()];
        int i = 0;
        for (Tag t : tags) {
            tagsArray[i] = t.getKey() + "=" + t.getValue();
            ++i;
        }
    }

    public Monitor getAnnotation() {
        return anno;
    }

    public AccessibleObject getAttribute() {
        return attr;
    }

    public TagList getTags() {
        return tags;
    }

    public String[] getTagsArray() {
        return tagsArray;
    }

    public Object getValue() throws Exception {
        return AnnotationUtils.getValue(obj, attr);
    }

    public Number getNumber() throws Exception {
        return AnnotationUtils.getNumber(obj, attr);
    }

    public AnnotatedAttribute copy(TagList classTags) {
        TagList newTags = BasicTagList.concat(classTags, tags);
        return new AnnotatedAttribute(obj, anno, attr, newTags);
    }
}

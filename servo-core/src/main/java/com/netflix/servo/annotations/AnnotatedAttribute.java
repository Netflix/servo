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

import com.netflix.servo.BasicTagList;
import com.netflix.servo.Tag;
import com.netflix.servo.TagList;

import java.lang.reflect.AccessibleObject;

/**
 * Wrapper around an {@link java.lang.reflect.AccessibleObject} that is
 * annotated with {@link Monitor}.
 */
public final class AnnotatedAttribute {

    private final Object obj;
    private final Monitor anno;
    private final AccessibleObject attr;
    private final TagList tags;
    private final String[] tagsArray;

    /** Creates a new instance. */
    public AnnotatedAttribute(Object obj, Monitor anno, AccessibleObject attr) {
        this(obj, anno, attr, BasicTagList.copyOf(anno.tags()));
    }

    /** Creates a new instance. */
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

    /** Returns the annotation on the attribute. */
    public Monitor getAnnotation() {
        return anno;
    }

    /** Returns the accessible object that is annotated. */
    public AccessibleObject getAttribute() {
        return attr;
    }

    /** Returns the tags for the attribute. */
    public TagList getTags() {
        return tags;
    }

    /** 
     * Returns the tags as an array of strings that can be parsed with
     * {#link com.netflix.servo.BasicTag#parseTag}.
     */
    public String[] getTagsArray() {
        return tagsArray;
    }

    /** Returns the current value for the attribute. */
    public Object getValue() throws Exception {
        return AnnotationUtils.getValue(obj, attr);
    }

    /** Returns the current value for the attribute as a number. */
    public Number getNumber() throws Exception {
        return AnnotationUtils.getNumber(obj, attr);
    }

    /** Returns the copy with the additional tags from the class. */
    public AnnotatedAttribute copy(TagList classTags) {
        TagList newTags = BasicTagList.concat(classTags, tags);
        return new AnnotatedAttribute(obj, anno, attr, newTags);
    }
}

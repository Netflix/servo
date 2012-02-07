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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.netflix.servo.TagList;

import java.util.List;

/**
 * Wrapper around an object that is annotated to make it easy to access the
 * annotated fields.
 */
public final class AnnotatedObject {

    private final Object object;
    private final String id;
    private final TagList tags;
    private final List<AnnotatedAttribute> attrs;

    public AnnotatedObject(Object obj) throws Exception {
        object = Preconditions.checkNotNull(obj);
        id = AnnotationUtils.getMonitorId(obj);
        tags = AnnotationUtils.getMonitorTags(obj);
        attrs = AnnotationUtils.getMonitoredAttributes(obj);
    }

    /** Returns the wrapped object. */
    public Object getObject() {
        return object;
    }

    /** Returns the id from the {@link MonitorId} annotation. */
    public String getId() {
        return id;
    }

    /** Returns the tags from the {@link MonitorTags} annotation. */
    public TagList getTags() {
        return tags;
    }

    /** Returns the attributes with {@link Monitor} annotations. */
    public List<AnnotatedAttribute> getAttributes() {
        return attrs;
    }

    /** Returns the canonical class name of the wrapped class. */
    public String getClassName() {
        return object.getClass().getCanonicalName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AnnotatedObject)) {
            return false;
        }
        AnnotatedObject annoObj = (AnnotatedObject) obj;
        return object == annoObj.getObject();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }
}

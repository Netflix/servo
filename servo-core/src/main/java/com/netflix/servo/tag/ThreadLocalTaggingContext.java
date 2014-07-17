/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.tag;

/**
 * Keeps track of tags that should be applied to counters incremented in the
 * current thread. Can be used to customize the context for code executed in
 * a particular thread. For example, on a server with a thread per request the
 * context can be set so metrics will be tagged accordingly.
 */
public final class ThreadLocalTaggingContext implements TaggingContext {

    private final ThreadLocal<TagList> context = new ThreadLocal<TagList>();

    private static final ThreadLocalTaggingContext INSTANCE = new ThreadLocalTaggingContext();

    /** Get the instance. */
    public static ThreadLocalTaggingContext getInstance() {
        return INSTANCE;
    }

    private ThreadLocalTaggingContext() {
    }

    /** Set the tags to be associated with the current thread. */
    public void setTags(TagList tags) {
        context.set(tags);
    }

    /** Get the tags associated with the current thread. */
    @Override
    public TagList getTags() {
        return context.get();
    }

    /** Remove the tags associated with the current thread. */
    public void reset() {
        context.remove();
    }
}

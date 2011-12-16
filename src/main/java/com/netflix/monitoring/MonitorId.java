/*
 * Copyright (c) 2011. Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.netflix.monitoring;

import javax.management.ObjectName;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used in creating the {@link ObjectName} of the {@link Monitor} annotated
 * object.  Use this annotation to uniquely identify your MBean.  For example,
 * if your class is like {@link ResourcePool} where you'll have multiple instances
 * created, you'll need to use this to uniquely identify each instance.
 *
 * @author gkim
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MonitorId {
}

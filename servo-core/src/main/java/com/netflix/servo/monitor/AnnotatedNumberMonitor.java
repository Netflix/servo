/*
 * Copyright 2011-2018 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

import com.netflix.servo.SpectatorContext;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.Throwables;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Wraps an annotated field and exposes it as a numeric monitor object.
 */
class AnnotatedNumberMonitor extends AbstractMonitor<Number>
    implements NumericMonitor<Number>, SpectatorMonitor {

  private final Object object;
  private final AccessibleObject field;

  AnnotatedNumberMonitor(MonitorConfig config, Object object, AccessibleObject field) {
    super(config);
    this.object = object;
    this.field = field;
    if ("COUNTER".equals(config.getTags().getValue("type"))) {
      SpectatorContext.polledGauge(config)
          .monitorMonotonicCounter(this, m -> m.getValue(0).longValue());
    } else {
      SpectatorContext.polledGauge(config)
          .monitorValue(this, m -> m.getValue(0).doubleValue());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Number getValue(int pollerIdx) {
    try {
      field.setAccessible(true);
      if (field instanceof Field) {
        return (Number) ((Field) field).get(object);
      } else {
        return (Number) ((Method) field).invoke(object);
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initializeSpectator(TagList tags) {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof AnnotatedNumberMonitor)) {
      return false;
    }
    AnnotatedNumberMonitor m = (AnnotatedNumberMonitor) obj;
    return config.equals(m.getConfig()) && field.equals(m.field);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = config.hashCode();
    result = 31 * result + field.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "AnnotatedNumberMonitor{config=" + config + ", field=" + field + '}';
  }
}

/**
 * Copyright 2018 Netflix, Inc.
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
package com.netflix.servo.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for dealing with reflection.
 */
public final class Reflection {

  private Reflection() {
  }

  /**
   * Gets all fields from class and its super classes.
   *
   * @param classs class to get fields from
   * @return set of fields
   */
  public static Set<Field> getAllFields(Class<?> classs) {
    Set<Field> set = new HashSet<>();
    Class<?> c = classs;
    while (c != null) {
      set.addAll(Arrays.asList(c.getDeclaredFields()));
      c = c.getSuperclass();
    }
    return set;
  }

  /**
   * Gets all methods from class and its super classes.
   *
   * @param classs class to get methods from
   * @return set of methods
   */
  public static Set<Method> getAllMethods(Class<?> classs) {
    Set<Method> set = new HashSet<>();
    Class<?> c = classs;
    while (c != null) {
      set.addAll(Arrays.asList(c.getDeclaredMethods()));
      c = c.getSuperclass();
    }
    return set;
  }

  /**
   * Gets all fields annotated by annotation.
   *
   * @param classs class to get fields from
   * @param ann    annotation that must be present on the field
   * @return set of fields
   */
  public static Set<Field> getFieldsAnnotatedBy(Class<?> classs, Class<? extends Annotation> ann) {
    Set<Field> set = new HashSet<>();
    for (Field field : getAllFields(classs)) {
      if (field.isAnnotationPresent(ann)) {
        set.add(field);
      }
    }
    return set;
  }

  /**
   * Gets all methods annotated by annotation.
   *
   * @param classs class to get fields from
   * @param ann    annotation that must be present on the method
   * @return set of methods
   */
  public static Set<Method> getMethodsAnnotatedBy(
      Class<?> classs, Class<? extends Annotation> ann) {
    Set<Method> set = new HashSet<>();
    for (Method method : getAllMethods(classs)) {
      if (method.isAnnotationPresent(ann)) {
        set.add(method);
      }
    }
    return set;
  }

}

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

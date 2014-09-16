package com.example.flow.util;

public final class ObjectUtils {
  private ObjectUtils() {
    // No instances.
  }

  /**
   * Workaround for IntelliJ's long-standing ambiguous method call bug.
   *
   * http://youtrack.jetbrains.com/issue/IDEA-72835
   */
  public static <T> Class<T> getClass(Object object) {
    //noinspection unchecked
    return (Class<T>) object.getClass();
  }

  public static String getHumanClassName(Object object) {
    Class<Object> c = getClass(object);
    String humanName = c.getSimpleName();
    if (c.isMemberClass()) {
      humanName = c.getDeclaringClass().getSimpleName() + "." + humanName;
    }
    return humanName;
  }
}

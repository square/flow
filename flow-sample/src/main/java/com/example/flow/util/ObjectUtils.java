/*
 * Copyright 2014 Square Inc.
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

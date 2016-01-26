/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package flow;

final class Preconditions {
  private Preconditions() {
    throw new AssertionError();
  }

  /**
   * @throws java.lang.IllegalArgumentException if condition is false.
   */
  static void checkArgument(boolean condition, String errorMessage) {
    if (!condition) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * @param reference an object reference
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  static <T> T checkNotNull(T reference, String errorMessage, Object... args) {
    if (reference == null) {
      throw new NullPointerException(String.format(errorMessage, args));
    }
    return reference;
  }
}

/*
 * Copyright 2016 Square Inc.
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

package flow;

/** Supplied by Flow to the Listener, which is responsible for calling onComplete(). */
public interface TraversalCallback {
  /**
   * Must be called exactly once to indicate that the corresponding transition has completed.
   *
   * If not called, the history will not be updated and further calls to Flow will not execute.
   * Calling more than once will result in an exception.
   */
  void onTraversalCompleted();
}

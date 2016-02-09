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

package flow;

public abstract class ServicesFactory {
  /**
   * Sets up any services associated with the key, and make them accessible via the context.
   * Typically this means returning a new context that wraps the given one.
   */
  public abstract void bindServices(Services.Binder services);

  /**
   * Tears down any services previously bound by {@link #bindServices}. Note that the Services
   * instance given here may be a wrapper around an instance that this factory created.
   */
  public void tearDownServices(Services services) {
  }
}

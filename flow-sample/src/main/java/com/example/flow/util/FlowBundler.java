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

import android.os.Bundle;
import flow.Backstack;
import flow.Flow;
import flow.Parceler;
import javax.annotation.Nullable;

/**
 * Handles Bundle persistence of a Flow.
 */
public abstract class FlowBundler {
  private static final String FLOW_KEY = "flow_key";

  private final Parceler parceler;

  private Flow flow;

  protected FlowBundler(Parceler parceler) {
    this.parceler = parceler;
  }

  public Flow onCreate(@Nullable Bundle savedInstanceState) {
    if (flow != null) return flow;

    Backstack restoredBackstack = null;
    if (savedInstanceState != null && savedInstanceState.containsKey(FLOW_KEY)) {
      restoredBackstack = Backstack.from(savedInstanceState.getParcelable(FLOW_KEY), parceler);
    }
    flow = new Flow(getColdStartBackstack(restoredBackstack));
    return flow;
  }

  public void onSaveInstanceState(Bundle outState) {
    Backstack backstack = getBackstackToSave(flow.getBackstack());
    if (backstack == null) return;
    outState.putParcelable(FLOW_KEY, backstack.getParcelable(parceler));
  }

  /**
   * Returns the backstack that should be archived by {@link #onSaveInstanceState}. Overriding
   * allows subclasses to handle cases where the current configuration is not one that should
   * survive process death.  The default implementation returns a BackStackToSave that specifies
   * that view state should be persisted.
   *
   * @return the stack to archive, or null to archive nothing
   */
  @Nullable protected Backstack getBackstackToSave(Backstack backstack) {
    return backstack;
  }

  /**
   * Returns the backstack to initialize the new flow.
   *
   * @param restoredBackstack the backstack recovered from the bundle passed to {@link #onCreate},
   * or null if there was no bundle or no backstack was found
   */
  protected abstract Backstack getColdStartBackstack(@Nullable Backstack restoredBackstack);
}

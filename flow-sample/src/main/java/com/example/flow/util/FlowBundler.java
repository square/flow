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

import static flow.Preconditions.checkArgument;

/**
 * Handles Bundle persistence of a Flow.
 */
public class FlowBundler {
  private static final String FLOW_KEY = "flow_key";

  private final Object defaultScreen;
  private final Flow.Dispatcher dispatcher;
  private final Parceler<Object> parceler;

  private Flow flow;

  public FlowBundler(Object defaultScreen, Flow.Dispatcher dispatcher, Parceler<Object> parceler) {
    this.dispatcher = dispatcher;
    this.defaultScreen = defaultScreen;
    this.parceler = parceler;
  }

  public Flow onCreate(Bundle savedInstanceState) {
    checkArgument(flow == null, "Flow already created.");
    Backstack backstack;
    if (savedInstanceState != null && savedInstanceState.containsKey(FLOW_KEY)) {
      backstack = Backstack.from(savedInstanceState.getParcelable(FLOW_KEY), parceler);
    } else {
      backstack = Backstack.fromUpChain(defaultScreen);
    }
    this.flow = new Flow(backstack, dispatcher);
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
  protected Backstack getBackstackToSave(Backstack backstack) {
    return backstack;
  }
}


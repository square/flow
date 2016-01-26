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

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;

import static flow.Preconditions.checkNotNull;

/**
 * A simple Dispatcher that only pays attention to the top keys on the incoming and outgoing
 * histories, and only executes a change if those top keys are not equal.
 */
public final class KeyDispatcher implements Flow.Dispatcher {

  public static final class Builder {
    private final Activity activity;
    @Nullable private KeyChanger keyChanger;

    public Builder(Activity activity) {
      this.activity = activity;
    }

    public Builder withKeyChanger(KeyChanger changer) {
      this.keyChanger = checkNotNull(changer, "KeyChanger may not be null");
      return this;
    }

    public Flow.Dispatcher build() {
      final KeyChanger keyChanger =
          this.keyChanger == null ? new DefaultKeyChanger(activity) : this.keyChanger;
      return new KeyDispatcher(activity, keyChanger);
    }
  }

  public static Builder configure(Activity activity) {
    return new Builder(activity);
  }

  private final Activity activity;
  private final KeyChanger keyChanger;

  private KeyDispatcher(Activity activity, KeyChanger keyChanger) {
    this.activity = activity;
    this.keyChanger = keyChanger;
  }

  @Override public void dispatch(Flow.Traversal traversal, Flow.TraversalCallback callback) {
    State inState = traversal.destination.topSaveState();
    Object inKey = inState.getKey();
    State outState = traversal.origin == null ? null : traversal.origin.topSaveState();
    Object outKey = outState == null ? null : outState.getKey();

    // TODO(#126): this short-circuit may belong in Flow, since every Dispatcher we have implements it.
    if (inKey.equals(outKey)) {
      callback.onTraversalCompleted();
    } else {
      changeKey(outState, inState, traversal.direction, traversal.createContext(inKey, activity),
          callback);
    }
  }

  public void changeKey(@Nullable State outgoingState, State incomingState,
      Flow.Direction direction, Context incomingContext, final Flow.TraversalCallback callback) {
    keyChanger.changeKey(outgoingState, incomingState, direction, incomingContext, callback);
  }
}

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

package flow.sample.basic;

import android.app.Activity;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import flow.Dispatcher;
import flow.Flow;
import flow.Traversal;
import flow.TraversalCallback;

final class BasicDispatcher implements Dispatcher {

  private final Activity activity;

  BasicDispatcher(Activity activity) {
    this.activity = activity;
  }

  @Override
  public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
    Log.d("BasicDispatcher", "dispatching " + traversal);
    Object destKey = traversal.destination.top();

    ViewGroup frame = (ViewGroup) activity.findViewById(R.id.basic_activity_frame);

    // We're already showing something, clean it up.
    if (frame.getChildCount() > 0) {
      final View currentView = frame.getChildAt(0);

      // Save the outgoing view state.
      if (traversal.origin != null) {
        traversal.getState(traversal.origin.top()).save(currentView);
      }

      // Short circuit if we would just be showing the same view again.
      final Object currentKey = Flow.getKey(currentView);
      if (destKey.equals(currentKey)) {
        callback.onTraversalCompleted();
        return;
      }

      frame.removeAllViews();
    }

    @LayoutRes final int layout;
    if (destKey instanceof HelloScreen) {
      layout = R.layout.hello_screen;
    } else if (destKey instanceof WelcomeScreen) {
      layout = R.layout.welcome_screen;
    } else {
      throw new AssertionError("Unrecognized screen " + destKey);
    }

    View incomingView = LayoutInflater.from(traversal.createContext(destKey, activity)) //
        .inflate(layout, frame, false);

    frame.addView(incomingView);
    traversal.getState(traversal.destination.top()).restore(incomingView);

    callback.onTraversalCompleted();
  }
}

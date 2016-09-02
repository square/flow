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

package flow.sample.orientation;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import flow.Dispatcher;
import flow.Traversal;
import flow.TraversalCallback;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

final class OrientationSampleDispatcher implements Dispatcher {
  /**
   * Using a static for this is a fragile hack for demo purposes. In
   * real life you'd want something hanging off of a custom application
   * class--perhaps a Mortar activity scope.
   */
  private static TraversalCallback hangingCallback;

  public static void finishPendingTraversal() {
    if (hangingCallback != null) {
      // The previous dispatcher was unable to finish its traversal because it
      // required a configuration change. Let flow know that we're awake again
      // in our new orientation and that traversal is done.

      TraversalCallback ref = hangingCallback;
      hangingCallback = null;
      ref.onTraversalCompleted();
    }
  }

  private final Activity activity;

  OrientationSampleDispatcher(Activity activity) {
    this.activity = activity;
  }

  @Override
  public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
    Log.d("BasicDispatcher", "dispatching " + traversal);

    final OrientationSampleScreen destScreen = traversal.destination.top();
    final boolean incomingNeedsLock = destScreen.requiresLandscape();
    final boolean waitForOrientationChange = incomingNeedsLock && requestLandscapeLock();

    if (waitForOrientationChange) {
      // There is about to be an orientation change, which means there will
      // soon be a new activity and a new dispatcher. Let them complete
      // this traversal, so that we don't try to show a landscape-only screen
      // in portrait.
      hangingCallback = callback;
    } else {
      ViewGroup frame = (ViewGroup) activity.findViewById(R.id.basic_activity_frame);
      View destView = LayoutInflater.from(traversal.createContext(destScreen, activity)) //
          .inflate(destScreen.getLayoutId(), frame, false);

      frame.removeAllViews();
      frame.addView(destView);

      if (!incomingNeedsLock) {
        requestUnlock();
      }
      callback.onTraversalCompleted();
    }
  }

  /**
   * Returns true if we've requested a lock and are expecting an orientation change
   * as a result.
   */
  boolean requestLandscapeLock() {
    int requestedOrientation = activity.getRequestedOrientation();

    if (requestedOrientation == SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
      // We're already locked.
      return false;
    }

    activity.setRequestedOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

    // We've requested a lock, but there will only be an orientation change
    // if we're not already landscape.
    return isPortrait();
  }

  void requestUnlock() {
    activity.setRequestedOrientation(SCREEN_ORIENTATION_UNSPECIFIED);
  }

  boolean isPortrait() {
    return activity.getResources().getBoolean(R.bool.is_portrait);
  }
}

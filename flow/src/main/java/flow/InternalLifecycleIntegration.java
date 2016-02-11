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
import android.app.Application;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import static flow.Preconditions.checkArgument;
import static flow.Preconditions.checkNotNull;

/**
 * Pay no attention to this class. It's only public because it has to be.
 */
public final class InternalLifecycleIntegration extends Fragment {
  static final String TAG = "flow-lifecycle-integration";

  static InternalLifecycleIntegration find(Activity activity) {
    return (InternalLifecycleIntegration) activity.getFragmentManager().findFragmentByTag(TAG);
  }

  static void install(final Application app, final Activity activity,
      @Nullable final KeyParceler parceler, final History defaultHistory,
      final Flow.Dispatcher dispatcher, final KeyManager keyManager) {
    app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
      @Override public void onActivityCreated(Activity a, Bundle savedInstanceState) {
        if (a == activity) {
          InternalLifecycleIntegration fragment = find(activity);
          boolean newFragment = fragment == null;
          if (newFragment) {
            fragment = new InternalLifecycleIntegration();
          }
          if (fragment.keyManager == null) {
            fragment.defaultHistory = defaultHistory;
            fragment.parceler = parceler;
            fragment.keyManager = keyManager;
          }
          // We always replace the dispatcher because it frequently references the Activity.
          fragment.dispatcher = dispatcher;
          if (newFragment) {
            activity.getFragmentManager() //
                .beginTransaction() //
                .add(fragment, TAG) //
                .commit();
          }
          app.unregisterActivityLifecycleCallbacks(this);
        }
      }

      @Override public void onActivityStarted(Activity activity) {
      }

      @Override public void onActivityResumed(Activity activity) {
      }

      @Override public void onActivityPaused(Activity activity) {
      }

      @Override public void onActivityStopped(Activity activity) {
      }

      @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
      }

      @Override public void onActivityDestroyed(Activity a) {
      }
    });
  }

  Flow flow;
  KeyManager keyManager;
  @Nullable KeyParceler parceler;
  History defaultHistory;
  Flow.Dispatcher dispatcher;
  Intent intent;
  private boolean dispatcherSet;

  public InternalLifecycleIntegration() {
    super();
    setRetainInstance(true);
  }

  void onNewIntent(Intent intent) {
    if (intent.hasExtra(Flow.HISTORY_KEY)) {
      History history = History.from(intent.getParcelableExtra(Flow.HISTORY_KEY),
          checkNotNull(parceler,
              "Intent has a Flow history extra, but Flow was not installed with a KeyParceler"));
      flow.setHistory(history, Flow.Direction.REPLACE);
    }
  }

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (flow == null) {
      History savedHistory = null;
      if (savedInstanceState != null && savedInstanceState.containsKey(Flow.HISTORY_KEY)) {
        savedHistory = History.from(savedInstanceState.getParcelable(Flow.HISTORY_KEY),
            checkNotNull(parceler, "no KeyParceler installed"));
      }
      History history = selectHistory(intent, savedHistory, defaultHistory, parceler);
      flow = new Flow(keyManager, history);
    }
    flow.setDispatcher(dispatcher);
    dispatcherSet = true;
  }

  @Override public void onResume() {
    super.onResume();
    if (!dispatcherSet) {
      flow.setDispatcher(dispatcher);
      dispatcherSet = true;
    }
  }

  @Override public void onPause() {
    flow.removeDispatcher(dispatcher);
    dispatcherSet = false;
    super.onPause();
  }

  @Override public void onDestroy() {
    keyManager.tearDown(flow.getHistory().top());
    super.onDestroy();
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    checkArgument(outState != null, "outState may not be null");
    if (parceler == null) {
      return;
    }

    Parcelable parcelable = flow.getHistory().getParcelable(parceler, new History.Filter() {
      @Override public boolean apply(Object state) {
        return !state.getClass().isAnnotationPresent(NotPersistent.class);
      }
    });
    if (parcelable != null) {
      //noinspection ConstantConditions
      outState.putParcelable(Flow.HISTORY_KEY, parcelable);
    }
  }

  private static History selectHistory(Intent intent, History saved, History defaultHistory,
      @Nullable KeyParceler parceler) {
    if (saved != null) {
      return saved;
    }
    if (intent != null && intent.hasExtra(Flow.HISTORY_KEY)) {
      checkNotNull(parceler,
          "Intent has a Flow history extra, but Flow was not installed with a KeyParceler");
      return History.from(intent.getParcelableExtra(Flow.HISTORY_KEY), parceler);
    }
    return defaultHistory;
  }
}

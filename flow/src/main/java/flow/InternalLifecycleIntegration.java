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

  static void install(Application app, final Activity activity,
      @Nullable final StateParceler parceler, final History defaultHistory,
      final Flow.Dispatcher dispatcher) {
    app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
          @Override public void onActivityCreated(Activity a, Bundle savedInstanceState) {
            if (a == activity) {
              InternalLifecycleIntegration fragment = find(activity);
              boolean newFragment = fragment == null;
              if (newFragment) {
                fragment = new InternalLifecycleIntegration();
              }
              fragment.defaultHistory = defaultHistory;
              fragment.dispatcher = dispatcher;
              fragment.parceler = parceler;
              if (newFragment) {
                activity.getFragmentManager()
                    .beginTransaction()
                    .add(fragment, TAG)
                    .commit();
              }
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

          @Override public void onActivityDestroyed(Activity activity) {
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
          }
        });
  }

  Flow flow;
  @Nullable StateParceler parceler;
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
              "Intent has a Flow history extra, but Flow was not installed with a StateParceler"));
      flow.setHistory(history, Flow.Direction.REPLACE);
    }
  }

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (flow == null) {
      History savedHistory = null;
      if (savedInstanceState != null && savedInstanceState.containsKey(Flow.HISTORY_KEY)) {
        savedHistory = History.from(savedInstanceState.getParcelable(Flow.HISTORY_KEY),
            checkNotNull(parceler, "no StateParceler installed"));
      }
      flow = new Flow(selectHistory(intent, savedHistory, defaultHistory, parceler));
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

  private static History selectHistory(Intent intent, History saved,
      History defaultHistory, @Nullable StateParceler parceler) {
    if (saved != null) {
      return saved;
    }
    if (intent != null && intent.hasExtra(Flow.HISTORY_KEY)) {
      checkNotNull(parceler,
          "Intent has a Flow history extra, but Flow was not installed with a StateParceler");
      return History.from(intent.getParcelableExtra(Flow.HISTORY_KEY), parceler);
    }
    return defaultHistory;
  }
}

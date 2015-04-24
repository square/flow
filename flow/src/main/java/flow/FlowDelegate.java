package flow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import static flow.Preconditions.checkArgument;

/**
 * Manages a Flow within an Activity.  Make sure that each method is called from the corresponding
 * method in the Activity.
 * <p>
 * Example:
 *
 * <pre>{@code
 * public class MainActivity extends Activity {
 *   private FlowDelegate activityFlowSupport;
 *   private final Flow.Dispatcher dispatcher = ...;
 *
 *   &#064;Override protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     Parceler parceler = new GsonParceler();
 *     History defaultHistory = History.single(new MyAppIntroScreen());
 *     FlowDelegate.NonConfigurationInstance nonConfig =
 *         (FlowDelegate.NonConfigurationInstance) getLastNonConfigurationInstance();
 *     flowSupport =
 *         FlowDelegate.onCreate(nonConfig, savedInstanceState, parceler, defaultHistory);
 *   }
 *
 *   &#064;Override public void onResume() {
 *     super.onResume();
 *     activityFlowSupport.onResume(flowDispatcher);
 *   }
 *
 *   &#064;Override protected void onPause() {
 *     activityFlowSupport.onPause();
 *     super.onPause();
 *   }
 *
 *   &#064;Override public Object onRetainNonConfigurationInstance() {
 *     return activityFlowSupport.onRetainNonConfigurationInstance();
 *   }
 *
 *   &#064;Override public void onSaveInstanceState(Bundle outState) {
 *     super.onSaveInstanceState(outState);
 *     activityFlowSupport.onSaveInstanceState(outState);
 *   }
 *
 *   &#064;Override public void onBackPressed() {
 *     if (!activityFlowSupport.onBackPressed()) {
 *       super.onBackPressed();
 *     }
 *   }
 *
 *   &#064;Override public Object getSystemService(String name) {
 *     Object service = activityFlowSupport.getSystemService(name);
 *     return service != null ? service : super.getSystemService(name);
 *   }
 * }
 * }</pre>
 */
public final class FlowDelegate {
  public static final class NonConfigurationInstance {
    private final Flow flow;

    public NonConfigurationInstance(Flow flow) {
      this.flow = flow;
    }
  }

  public static void setHistoryExtra(Intent intent, History history, StateParceler parceler) {
    intent.putExtra(HISTORY_KEY, history.getParcelable(parceler));
  }

  private static final String HISTORY_KEY =
      FlowDelegate.class.getSimpleName() + "_history";

  private final StateParceler parceler;
  private final Flow flow;
  private Flow.Dispatcher dispatcher;
  private boolean dispatcherSet;

  private FlowDelegate(Flow flow, Flow.Dispatcher dispatcher, StateParceler parceler) {
    this.flow = flow;
    this.dispatcher = dispatcher;
    this.parceler = parceler;
  }

  /** Immediately starts the Dispatcher, so the dispatcher should be prepared before calling. */
  public static FlowDelegate onCreate(NonConfigurationInstance nonConfigurationInstance,
      Intent intent, Bundle savedInstanceState, StateParceler parceler, History defaultHistory,
      Flow.Dispatcher dispatcher) {
    checkArgument(parceler != null, "parceler may not be null");
    final Flow flow;
    if (nonConfigurationInstance != null) {
      flow = nonConfigurationInstance.flow;
    } else {
      History savedHistory = null;
      if (savedInstanceState != null && savedInstanceState.containsKey(HISTORY_KEY)) {
        savedHistory = History.from(savedInstanceState.getParcelable(HISTORY_KEY), parceler);
      }
      flow = new Flow(selectHistory(intent, savedHistory, defaultHistory, parceler));
    }
    flow.setDispatcher(dispatcher);
    return new FlowDelegate(flow, dispatcher, parceler);
  }

  public void onNewIntent(Intent intent) {
    checkArgument(intent != null, "intent may not be null");
    if (intent.hasExtra(HISTORY_KEY)) {
      History history = History.from(intent.getParcelableExtra(HISTORY_KEY), parceler);
      flow.setHistory(history, Flow.Direction.REPLACE);
    }
  }

  public void onResume() {
    if (!dispatcherSet) {
      dispatcherSet = true;
      flow.setDispatcher(dispatcher);
    }
  }

  public NonConfigurationInstance onRetainNonConfigurationInstance() {
    return new NonConfigurationInstance(flow);
  }

  public void onPause() {
    flow.removeDispatcher(dispatcher);
    dispatcherSet = false;
  }

  /**
   * @return true if the button press has been handled.
   */
  public boolean onBackPressed() {
    return flow.goBack();
  }

  public void onSaveInstanceState(Bundle outState) {
    checkArgument(outState != null, "outState may not be null");
    Parcelable parcelable = flow.getHistory().getParcelable(parceler, new History.Filter() {
      @Override public boolean apply(Object state) {
        return !state.getClass().isAnnotationPresent(NotPersistent.class);
      }
    });
    if (parcelable != null) {
      //noinspection ConstantConditions
      outState.putParcelable(HISTORY_KEY, parcelable);
    }
  }

  /**
   * @return The requested service, or null if it does not exist
   */
  public Object getSystemService(String name) {
    if (Flow.isFlowSystemService(name)) {
      return flow;
    }
    return null;
  }

  private static History selectHistory(Intent intent, History saved,
      History defaultHistory, StateParceler parceler) {
    if (intent.hasExtra(HISTORY_KEY)) {
      return History.from(intent.getParcelableExtra(HISTORY_KEY), parceler);
    }
    if (saved != null) {
      return saved;
    }
    return defaultHistory;
  }
}

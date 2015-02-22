package flow;

import android.os.Bundle;
import java.util.Iterator;

import static flow.Preconditions.checkArgument;
import static flow.Preconditions.checkState;

/**
 * Manages a Flow within an Activity.  Make sure that each method is called from the corresponding
 * method in the Activity.
 * <p>
 * Example:
 *
 * <pre>{@code
 * public class MainActivity extends Activity {
 *   private ActivityFlowSupport activityFlowSupport;
 *   private final Flow.Dispatcher dispatcher = ...;
 *
 *   &#064;Override protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     Parceler parceler = new GsonParceler();
 *     Backstack defaultBackstack = Backstack.single(new MyAppIntroScreen());
 *     ActivityFlowSupport.NonConfigurationInstance nonConfig =
 *         (ActivityFlowSupport.NonConfigurationInstance) getLastNonConfigurationInstance();
 *     flowSupport =
 *         ActivityFlowSupport.onCreate(nonConfig, savedInstanceState, parceler, defaultBackstack);
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
public final class ActivityFlowSupport {
  public static final class NonConfigurationInstance {
    private final Flow flow;

    public NonConfigurationInstance(Flow flow) {
      this.flow = flow;
    }
  }

  private static final String FLOW_KEY = ActivityFlowSupport.class.getSimpleName() + "_backstack";

  private final Flow flow;
  private final Parceler parceler;
  private Flow.Dispatcher dispatcher;

  private ActivityFlowSupport(Flow flow, Parceler parceler) {
    this.flow = flow;
    this.parceler = parceler;
  }

  public static ActivityFlowSupport onCreate(NonConfigurationInstance nonConfigurationInstance,
      Bundle savedInstanceState, Parceler parceler, Backstack defaultBackstack) {
    if (nonConfigurationInstance != null) {
      return new ActivityFlowSupport(nonConfigurationInstance.flow, parceler);
    }
    checkArgument(parceler != null, "parceler may not be null");
    checkArgument(defaultBackstack != null, "defaultBackstack may not be null");
    Backstack initialBackstack = null;
    if (savedInstanceState != null && savedInstanceState.containsKey(FLOW_KEY)) {
      initialBackstack = Backstack.from(savedInstanceState.getParcelable(FLOW_KEY), parceler);
    }
    if (initialBackstack == null) {
      initialBackstack = defaultBackstack;
    }
    return new ActivityFlowSupport(new Flow(initialBackstack), parceler);
  }

  public NonConfigurationInstance onRetainNonConfigurationInstance() {
    return new NonConfigurationInstance(flow);
  }

  public void onResume(Flow.Dispatcher dispatcher) {
    checkState(flow != null, "Don't have a Flow. Did you forget to call onCreate()?");
    checkArgument(dispatcher != null, "dispatcher may not be null");
    this.dispatcher = dispatcher;
    flow.setDispatcher(dispatcher);
  }

  public void onPause() {
    checkState(flow != null, "Don't have a Flow. Did you forget to call onCreate()?");
    checkState(dispatcher != null, "Don't have a Dispatcher. Did you forget to call onResume()?");
    flow.removeDispatcher(dispatcher);
    dispatcher = null;
  }

  /**
   * @return true if the button press has been handled.
   */
  public boolean onBackPressed() {
    checkState(flow != null, "Don't have a Flow. Did you forget to call onCreate()?");
    return flow.goBack();
  }

  public void onSaveInstanceState(Bundle outState) {
    checkArgument(outState != null, "outState may not be null");
    checkState(flow != null, "Don't have a Flow. Did you forget to call onCreate()?");
    Backstack backstack = getBackstackToSave(flow.getBackstack());
    if (backstack == null) return;
    //noinspection ConstantConditions
    outState.putParcelable(FLOW_KEY, backstack.getParcelable(parceler));
  }

  /**
   * @return The requested service, or null if it does not exist
   */
  public Object getSystemService(String name) {
    if (Flow.isFlowSystemService(name)) {
      checkState(flow != null, "Don't have a Flow. Did you forget to call onCreate()?");
      return flow;
    }
    return null;
  }

  private Backstack getBackstackToSave(Backstack backstack) {
    Iterator<Path> it = backstack.reverseIterator();
    Backstack.Builder save = Backstack.emptyBuilder();
    boolean empty = true;
    while (it.hasNext()) {
      Path path = it.next();
      if (!path.getClass().isAnnotationPresent(NotPersistent.class)) {
        save.push(path);
        empty = false;
      }
    }
    return empty ? null : save.build();
  }
}


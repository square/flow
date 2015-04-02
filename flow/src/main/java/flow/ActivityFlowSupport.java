package flow;

import android.content.Intent;
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

  public static void setBackstackExtra(Intent intent, Backstack backstack, Parceler parceler) {
    intent.putExtra(BACKSTACK_KEY, backstack.getParcelable(parceler));
  }

  private static final String BACKSTACK_KEY =
      ActivityFlowSupport.class.getSimpleName() + "_backstack";

  private final Parceler parceler;
  private final Flow flow;
  private Flow.Dispatcher dispatcher;

  private ActivityFlowSupport(Flow flow, Parceler parceler) {
    this.flow = flow;
    this.parceler = parceler;
  }

  public static ActivityFlowSupport onCreate(NonConfigurationInstance nonConfigurationInstance,
      Intent intent, Bundle savedInstanceState, Parceler parceler, Backstack defaultBackstack) {
    checkArgument(parceler != null, "parceler may not be null");
    final Flow flow;
    if (nonConfigurationInstance != null) {
      flow = nonConfigurationInstance.flow;
    } else {
      Backstack savedBackstack = null;
      if (savedInstanceState != null && savedInstanceState.containsKey(BACKSTACK_KEY)) {
        savedBackstack = Backstack.from(savedInstanceState.getParcelable(BACKSTACK_KEY), parceler);
      }
      flow = new Flow(selectBackstack(intent, savedBackstack, defaultBackstack, parceler));
    }
    return new ActivityFlowSupport(flow, parceler);
  }

  public void onNewIntent(Intent intent) {
    checkArgument(intent != null, "intent may not be null");
    if (intent.hasExtra(BACKSTACK_KEY)) {
      Backstack backstack = Backstack.from(intent.getParcelableExtra(BACKSTACK_KEY), parceler);
      flow.setBackstack(backstack, Flow.Direction.REPLACE);
    }
  }

  public void onResume(Flow.Dispatcher dispatcher) {
    checkArgument(dispatcher != null, "dispatcher may not be null");
    this.dispatcher = dispatcher;

    flow.setDispatcher(dispatcher);
  }

  public NonConfigurationInstance onRetainNonConfigurationInstance() {
    return new NonConfigurationInstance(flow);
  }

  public void onPause() {
    checkState(dispatcher != null, "Did you forget to call onResume()?");
    flow.removeDispatcher(dispatcher);
    dispatcher = null;
  }

  /**
   * @return true if the button press has been handled.
   */
  public boolean onBackPressed() {
    return flow.goBack();
  }

  public void onSaveInstanceState(Bundle outState) {
    checkArgument(outState != null, "outState may not be null");
    Backstack backstack = getBackstackToSave(flow.getBackstack());
    if (backstack == null) return;
    //noinspection ConstantConditions
    outState.putParcelable(BACKSTACK_KEY, backstack.getParcelable(parceler));
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

  private static Backstack selectBackstack(Intent intent, Backstack saved,
      Backstack defaultBackstack, Parceler parceler) {
    if (intent.hasExtra(BACKSTACK_KEY)) {
      return Backstack.from(intent.getParcelableExtra(BACKSTACK_KEY), parceler);
    }
    if (saved != null) {
      return saved;
    }
    return defaultBackstack;
  }

  private static Backstack getBackstackToSave(Backstack backstack) {
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

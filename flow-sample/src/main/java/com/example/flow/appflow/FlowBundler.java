package com.example.flow.appflow;

import android.os.Bundle;
import android.os.Parcelable;
import flow.Backstack;
import flow.Flow;
import flow.Parcer;

import static com.example.flow.util.Preconditions.checkArgument;

/**
 * Handles Bundle persistence of a Flow.
 */
public class FlowBundler {
  private static final Parcer<Object> PARCER = new Parcer<Object>() {
    @Override public Parcelable wrap(Object instance) {
      return (Parcelable) instance;
    }

    @Override public Object unwrap(Parcelable parcelable) {
      return parcelable;
    }
  };
  private static final String FLOW_KEY = "flow_key";

  private final Flow.Listener listener;
  private final Object defaultScreen;

  private Flow flow;

  public FlowBundler(Flow.Listener listener, Object defaultScreen) {
    this.listener = listener;
    this.defaultScreen = defaultScreen;
  }

  public AppFlow.FlowHolder onCreate(Bundle savedInstanceState) {
    checkArgument(flow == null, "Flow already created.");
    Backstack backstack;
    if (savedInstanceState != null && savedInstanceState.containsKey(FLOW_KEY)) {
      backstack = Backstack.from(savedInstanceState.getParcelable(FLOW_KEY), PARCER);
    } else {
      backstack = Backstack.fromUpChain(defaultScreen);
    }
    flow = new Flow(backstack, listener);
    return new AppFlow.FlowHolder(flow);
  }

  public void onSaveInstanceState(Bundle outState) {
    Backstack backstack = getBackstackToSave(flow.getBackstack());
    if (backstack == null) return;
    outState.putParcelable(FLOW_KEY, backstack.getParcelable(PARCER));
  }

  public final Flow getFlow() {
    return flow;
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


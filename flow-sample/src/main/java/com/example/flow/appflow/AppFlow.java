package com.example.flow.appflow;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import flow.Flow;

/**
 * Builds a hierarchy of {@link Flow}s.  Handles delegating to parent when an unrecognized
 * screen is requested.
 */
public abstract class AppFlow {
  public static Flow get(Context context) {
    return FlowHolder.get(context).flow;
  }

  public static <T> T getScreen(Context context) {
    // If this blows up, it's on the caller.  We hide the cast as a convenience.
    //noinspection unchecked
    return (T) LocalScreenWrapper.get(context).localScreen;
  }

  public static void loadInitialScreen(Context context) {
    Flow flow = get(context);
    //noinspection unchecked
    Object screen = (Object) get(context).getBackstack().current().getScreen();
    flow.resetTo(screen);
  }

  public static boolean isFlowHolderSystemService(String name) {
    return FlowHolder.FLOW_HOLDER_SERVICE.equals(name);
  }

  static Context setScreen(Context context, Object screen) {
    return new LocalScreenWrapper(context, screen);
  }

  public static final class FlowHolder {
    static final String FLOW_HOLDER_SERVICE = "flow_holder";

    static FlowHolder get(Context context) {
      //noinspection ResourceType
      return (FlowHolder) context.getSystemService(FLOW_HOLDER_SERVICE);
    }

    final Flow flow;

    FlowHolder(Flow flow) {
      this.flow = flow;
    }
  }

  private static final class LocalScreenWrapper extends ContextWrapper {
    static final String LOCAL_WRAPPER_SERVICE = "flow_local_screen_context_wrapper";
    private LayoutInflater inflater;

    static LocalScreenWrapper get(Context context) {
      //noinspection ResourceType
      return (LocalScreenWrapper) context.getSystemService(LOCAL_WRAPPER_SERVICE);
    }

    final Object localScreen;

    LocalScreenWrapper(Context base, Object localScreen) {
      super(base);
      this.localScreen = localScreen;
    }

    @Override public Object getSystemService(String name) {
      if (LOCAL_WRAPPER_SERVICE.equals(name)) {
        return this;
      }
      if (LAYOUT_INFLATER_SERVICE.equals(name)) {
        if (inflater == null) {
          inflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
        }
        return inflater;
      }
      return super.getSystemService(name);
    }
  }
}


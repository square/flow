package com.example.flow.appflow;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import flow.Flow;

public final class AppFlow {
  private static final String APP_FLOW_SERVICE = "app_flow";

  private final Flow flow;

  protected AppFlow(Flow flow) {
    this.flow = flow;
  }

  public static Flow get(Context context) {
    AppFlow appFlow =  (AppFlow) context.getSystemService(APP_FLOW_SERVICE);
    return appFlow.flow;
  }

  public static <T> T getScreen(Context context) {
    // If this blows up, it's on the caller.  We hide the cast as a convenience.
    //noinspection unchecked
    return (T) LocalScreenWrapper.get(context).localScreen;
  }

  public static void loadInitialScreen(Context context) {
    Flow flow = get(context);
    Object screen = get(context).getBackstack().current().getScreen();
    flow.resetTo(screen);
  }

  public static boolean isAppFlowSystemService(String name) {
    return APP_FLOW_SERVICE.equals(name);
  }

  static Context setScreen(Context context, Object screen) {
    return new LocalScreenWrapper(context, screen);
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


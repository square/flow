package flow;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.Nullable;

public final class Installer {

  private final Context baseContext;
  private final Activity activity;
  private KeyParceler parceler;
  private Object defaultKey;
  private Flow.Dispatcher dispatcher;

  public Installer(Context baseContext, Activity activity) {
    this.baseContext = baseContext;
    this.activity = activity;
  }

  public Installer keyParceler(@Nullable KeyParceler parceler) {
    this.parceler = parceler;
    return this;
  }

  public Installer dispatcher(@Nullable Flow.Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
    return this;
  }

  public Installer defaultKey(@Nullable Object defaultKey) {
    this.defaultKey = defaultKey;
    return this;
  }

  public Context install() {
    if (InternalLifecycleIntegration.find(activity) != null) {
      throw new IllegalStateException("Flow is already installed in this Activity.");
    }
    final Flow.Dispatcher dis = dispatcher == null ? new DefaultDispatcher(activity) : dispatcher;
    final Object defState = defaultKey == null ? "Hello, World!" : defaultKey;

    final History defaultHistory =
        History.single(defState);
    final Application app = (Application) baseContext.getApplicationContext();
    InternalLifecycleIntegration.install(app, activity, parceler, defaultHistory, dis);

    return new InternalContextWrapper(baseContext, activity);
  }
}

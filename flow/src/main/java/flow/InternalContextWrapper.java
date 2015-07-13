package flow;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

final class InternalContextWrapper extends ContextWrapper {
  private static final String FLOW_SERVICE = "flow.InternalContextWrapper.FLOW_SERVICE";

  static Flow getFlow(Context context) {
    //noinspection ResourceType
    return (Flow) context.getSystemService(FLOW_SERVICE);
  }

  private final Activity activity;
  private Flow flow;

  InternalContextWrapper(Context baseContext, Activity activity) {
    super(baseContext);
    this.activity = activity;
  }

  @Override public Object getSystemService(String name) {
    if (FLOW_SERVICE.equals(name)) {
      if (flow == null) {
        flow = InternalLifecycleIntegration.find(activity).flow;
      }
      return flow;
    }
    return super.getSystemService(name);
  }
}

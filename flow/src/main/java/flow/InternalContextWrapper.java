package flow;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

final class InternalContextWrapper extends ContextWrapper {
  private static final String FLOW_SERVICE = "flow.InternalContextWrapper.FLOW_SERVICE";

  static Flow getFlow(Context context) {
    @SuppressWarnings("WrongConstant")
    Flow systemService = (Flow) context.getSystemService(FLOW_SERVICE);
    return systemService;
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

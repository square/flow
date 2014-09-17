package com.example.flow.screenswitcher;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;

/**
 * A shim context whose layout inflater will be based on a context that can be switched
 * dynamically.
 *
 * Useful for transition scenes: these are generated at startup time and cached, and they hold a
 * context for view inflation. We set the inflater context right before a scene inflates a new
 * view, and then clear it. This context can safely be shared by multiple scenes.
 */
public final class InflaterShimContext extends ContextWrapper {
  private LayoutInflater inflater;

  public InflaterShimContext(Context base) {
    super(base);
  }

  public void setInflaterContext(Context context) {
    // We cloneInContext to ensure that views we inflate use the given context, not whatever wrapped
    // context further up the hierarchy actually supplies the inflater to from().
    inflater = LayoutInflater.from(context).cloneInContext(context);
  }

  public void clearInflater() {
    inflater = null;
  }

  @Override public Object getSystemService(String name) {
    if (LAYOUT_INFLATER_SERVICE.equals(name)) {
      if (inflater == null) {
        throw new IllegalStateException("No inflating should happen after clearInflater()");
      }
      return inflater;
    }

    return super.getSystemService(name);
  }
}

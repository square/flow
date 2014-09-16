package com.example.flow.screenswitcher;

import android.view.View;
import flow.Flow;

/**
 * Support for {@link HandlesUp} and {@link HandlesBack}.
 */
public class UpAndBackHandler {
  private final Flow flow;

  public UpAndBackHandler(Flow flow) {
    this.flow = flow;
  }

  public boolean onUpPressed(View childView) {
    if (childView instanceof HandlesUp) {
      if (((HandlesUp) childView).onUpPressed()) {
        return true;
      }
    }
    // Try to go up.  If up isn't supported, go back.
    return flow.goUp() || onBackPressed(childView);
  }

  public boolean onBackPressed(View childView) {
    if (childView instanceof HandlesBack) {
      if (((HandlesBack) childView).onBackPressed()) {
        return true;
      }
    }
    return flow.goBack();
  }
}

package flow.sample.orientation;

import android.support.annotation.LayoutRes;

abstract class OrientationSampleScreen {
  @LayoutRes abstract int getLayoutId();

  boolean requiresLandscape() {
    return false;
  }
}

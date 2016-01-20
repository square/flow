package flow;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

public abstract class Saved {
  public static final Saved NULL = new Saved() {
    @Override public void save(View view) {
    }

    @Override public void restore(View view) {
    }
  };
  private Bundle bundle;

  Saved() {
    // No external instances.
  }

  public abstract void save(View view);

  public abstract void restore(View view);

  public void setBundle(Bundle bundle) {
    this.bundle = bundle;
  }

  @Nullable public Bundle getBundle() {
    return bundle;
  }
}

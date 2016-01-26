package flow;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;

public class State {
  /** Creates a State instance that has no state and is effectively immutable. */
  public static State empty(final Object key) {
    return new EmptyState(key);
  }

  private final Object key;
  private Bundle bundle;
  @Nullable SparseArray<Parcelable> viewState;

  State(Object key) {
    // No external instances.
    this.key = key;
  }

  public final <T> T getKey() {
    @SuppressWarnings("unchecked") final T state = (T) key;
    return state;
  }

  public void save(View view) {
    SparseArray<Parcelable> state = new SparseArray<>();
    view.saveHierarchyState(state);
    viewState = state;
  }

  public void restore(View view) {
    if (viewState != null) {
      view.restoreHierarchyState(viewState);
    }
  }

  public void setBundle(Bundle bundle) {
    this.bundle = bundle;
  }

  @Nullable public Bundle toBundle() {
    return bundle;
  }

  Bundle toBundle(KeyParceler parceler) {
    Bundle bundle = new Bundle();
    bundle.putParcelable("KEY", parceler.toParcelable(getKey()));
    bundle.putSparseParcelableArray("VIEW_STATE", viewState);
    bundle.putBundle("BUNDLE", bundle);
    return bundle;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    State state = (State) o;
    return (getKey().equals(state.getKey()));
  }

  @Override public int hashCode() {
    return getKey().hashCode();
  }

  @Override public String toString() {
    return getKey().toString();
  }

  private static final class EmptyState extends State {
    public EmptyState(Object flowState) {
      super(flowState);
    }

    @Override public void save(View view) {
    }

    @Override public void restore(View view) {
    }

    @Override public void setBundle(Bundle bundle) {
    }

    @Nullable @Override public Bundle toBundle() {
      return null;
    }
  }
}

/*
 * Copyright 2016 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package flow;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;
import java.util.LinkedHashMap;
import java.util.Map;

import static flow.Preconditions.checkNotNull;

public class State {
  private static final String VIEW_STATE_IDS = "VIEW_STATE_IDS";
  private static final String BUNDLE = "BUNDLE";
  private static final String VIEW_STATE_PREFIX = "VIEW_STATE_";
  private static final String KEY = "KEY";

  /** Creates a State instance that has no state and is effectively immutable. */
  @NonNull public static State empty(@NonNull final Object key) {
    return new EmptyState(key);
  }

  @NonNull static State fromBundle(@NonNull Bundle savedState, @NonNull KeyParceler parceler) {
    Object key = parceler.toKey(savedState.getParcelable(KEY));
    State state = new State(key);
    int[] viewIds = checkNotNull(savedState.getIntArray(VIEW_STATE_IDS), "Null view state ids?");
    for (int viewId : viewIds) {
      SparseArray<Parcelable> viewState =
          savedState.getSparseParcelableArray(VIEW_STATE_PREFIX + viewId);
      if (viewState != null) {
        state.viewStateById.put(viewId, viewState);
      }
    }
    state.bundle = savedState.getBundle(BUNDLE);
    return state;
  }

  private final Object key;
  @Nullable private Bundle bundle;
  // TODO shouldn't this be private?
  @NonNull Map<Integer, SparseArray<Parcelable>> viewStateById = new LinkedHashMap<>();

  State(Object key) {
    // No external instances.
    this.key = key;
  }

  @NonNull public final <T> T getKey() {
    @SuppressWarnings("unchecked") final T state = (T) key;
    return state;
  }

  /**
   * Save view hierarchy state so it can be restored later from {@link #restore(View)}.  The view
   * must have a non-zero id.
   */
  public void save(@NonNull View view) {
    int viewId = view.getId();
    Preconditions.checkArgument(viewId != 0,
        "Cannot save state for View with no id " + view.getClass().getSimpleName());
    SparseArray<Parcelable> state = new SparseArray<>();
    view.saveHierarchyState(state);
    viewStateById.put(viewId, state);
  }

  public void restore(@NonNull View view) {
    SparseArray<Parcelable> viewState = viewStateById.get(view.getId());
    if (viewState != null) {
      view.restoreHierarchyState(viewState);
    }
  }

  public void setBundle(@Nullable Bundle bundle) {
    this.bundle = bundle;
  }

  @Nullable public Bundle getBundle() {
    return bundle;
  }

  Bundle toBundle(KeyParceler parceler) {
    Bundle outState = new Bundle();
    outState.putParcelable(KEY, parceler.toParcelable(getKey()));
    int[] viewIds = new int[viewStateById.size()];
    int c = 0;
    for (Map.Entry<Integer, SparseArray<Parcelable>> entry : viewStateById.entrySet()) {
      Integer viewId = entry.getKey();
      viewIds[c++] = viewId;
      SparseArray<Parcelable> viewState = entry.getValue();
      if (viewState.size() > 0) {
        outState.putSparseParcelableArray(VIEW_STATE_PREFIX + viewId, viewState);
      }
    }
    outState.putIntArray(VIEW_STATE_IDS, viewIds);
    if (bundle != null && !bundle.isEmpty()) {
      outState.putBundle(BUNDLE, bundle);
    }
    return outState;
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
    EmptyState(Object flowState) {
      super(flowState);
    }

    @Override public void save(@NonNull View view) {
    }

    @Override public void restore(@NonNull View view) {
    }

    @Override public void setBundle(Bundle bundle) {
    }

    @Nullable @Override public Bundle getBundle() {
      return null;
    }
  }
}

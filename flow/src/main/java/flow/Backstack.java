/*
 * Copyright 2013 Square Inc.
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
import android.util.SparseArray;
import android.view.View;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;

/**
 * Describes the history of a {@link Flow} at a specific point in time.
 */
public final class Backstack implements Iterable<Object> {
  public static interface Filter {
    boolean apply(Object state);
  }

  /** Restore a saved backstack from a {@link Parcelable} using the supplied {@link StateParceler}. */
  public static Backstack from(Parcelable parcelable, StateParceler parceler) {
    Bundle bundle = (Bundle) parcelable; // TODO(loganj): assert/throw
    ArrayList<Bundle> entryBundles = bundle.getParcelableArrayList("ENTRIES");
    Deque<Entry> entries = new ArrayDeque<>(entryBundles.size());
    for (Bundle entryBundle : entryBundles) {
      Object object = parceler.unwrap(entryBundle.getParcelable("OBJECT"));
      Entry entry = new Entry(object);
      entry.viewState = entryBundle.getSparseParcelableArray("VIEW_STATE");
      entries.add(entry);
    }
    return new Backstack(entries);
  }

  private final Deque<Entry> backstack;

  /** Get a {@link Parcelable} of this backstack using the supplied {@link StateParceler}. */
  public Parcelable getParcelable(StateParceler parceler) {
    Bundle backstackBundle = new Bundle();
    ArrayList<Bundle> entryBundles = new ArrayList<>(backstack.size());
    for (Entry entry : backstack) {
     entryBundles.add(entry.getBundle(parceler));
    }
    backstackBundle.putParcelableArrayList("ENTRIES", entryBundles);
    return backstackBundle;
  }

  /**
   * Get a {@link Parcelable} of this backstack using the supplied {@link StateParceler}, filtered
   * by the supplied {@link Filter}.
   *
   * The filter is invoked on each state in the stack in reverse order
   *
   * @return null if all states are filtered out.
   */
  public Parcelable getParcelable(StateParceler parceler, Filter filter) {
    Bundle backstackBundle = new Bundle();
    ArrayList<Bundle> entryBundles = new ArrayList<>(backstack.size());
    Iterator<Entry> it = backstack.descendingIterator();
    while (it.hasNext()) {
      Entry entry = it.next();
      if (filter.apply(entry.state)) {
        entryBundles.add(entry.getBundle(parceler));
      }
    }
    Collections.reverse(entryBundles);
    backstackBundle.putParcelableArrayList("ENTRIES", entryBundles);
    return backstackBundle;
  }

  public static Builder emptyBuilder() {
    return new Builder(Collections.<Entry>emptyList());
  }

  /** Create a backstack that contains a single object. */
  public static Backstack single(Object object) {
    return emptyBuilder().push(object).build();
  }

  private Backstack(Deque<Entry> backstack) {
    this.backstack = backstack;
  }

  @SuppressWarnings("UnusedDeclaration")
  @Override public Iterator<Object> iterator() {
    return new ReadIterator<>(backstack.iterator());
  }

  public <T> Iterator<T> reverseIterator() {
    return new ReadIterator<>(backstack.descendingIterator());
  }


  public int size() {
    return backstack.size();
  }

  public <T> T top() {
    //noinspection unchecked
    return (T) backstack.peek().state;
  }

  public ViewState currentViewState() {
    return backstack.peek();
  }

  /** Get a builder to modify a copy of this backstack. */
  public Builder buildUpon() {
    return new Builder(backstack);
  }

  @Override public String toString() {
    return backstack.toString();
  }

  private static final class Entry implements ViewState {
    final Object state;
    SparseArray<Parcelable> viewState;

    Entry(Object state) {
      this.state = state;
    }

    @Override public void save(View view) {
      SparseArray<Parcelable> state = new SparseArray<>();
      view.saveHierarchyState(state);
      viewState = state;
    }

    @Override public void restore(View view) {
      if (viewState != null) {
        view.restoreHierarchyState(viewState);
      }
    }

    Bundle getBundle(StateParceler parceler) {
      Bundle bundle = new Bundle();
      bundle.putParcelable("OBJECT", parceler.wrap(state));
      bundle.putSparseParcelableArray("VIEW_STATE", viewState);
      return bundle;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Entry entry = (Entry) o;
      return (state.equals(entry.state));
    }

    @Override
    public int hashCode() {
      return state.hashCode();
    }
  }

  public static final class Builder {
    private final Deque<Entry> backstack;

    private Builder(Collection<Entry> backstack) {
      this.backstack = new ArrayDeque<>(backstack);
    }

    public Builder push(Object object) {
      Entry entry = new Entry(object);
      backstack.push(entry);

      return this;
    }

    public Builder addAll(Collection<Object> c) {
      for (Object object : c) {
        backstack.push(new Entry(object));
      }

      return this;
    }

    public Object peek() {
      return backstack.peek().state;
    }

    public Object pop() {
      return backstack.pop().state;
    }

    public Backstack build() {
      if (backstack.isEmpty()) {
        throw new IllegalStateException("Backstack may not be empty");
      }

      return new Backstack(backstack);
    }
  }

  private static class ReadIterator<T> implements Iterator<T> {
    private final Iterator<Entry> iterator;

    public ReadIterator(Iterator<Entry> iterator) {
      this.iterator = iterator;
    }

    @Override public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override public T next() {
      //noinspection unchecked
      return (T) iterator.next().state;
    }

    @Override public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}

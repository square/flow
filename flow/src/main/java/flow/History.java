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
import java.util.LinkedHashMap;
import java.util.Map;

import static flow.Preconditions.checkArgument;

/**
 * Describes the history of a {@link Flow} at a specific point in time.
 */
public final class History implements Iterable<Object> {
  public interface Filter {
    boolean apply(Object state);
  }

  /** Restore a saved history from a {@link Parcelable} using the supplied {@link StateParceler}. */
  public static History from(Parcelable parcelable, StateParceler parceler) {
    Bundle bundle = (Bundle) parcelable; // TODO(loganj): assert/throw
    ArrayList<Bundle> entryBundles = bundle.getParcelableArrayList("ENTRIES");
    Deque<Entry> entries = new ArrayDeque<>(entryBundles.size());
    for (Bundle entryBundle : entryBundles) {
      Object object = parceler.unwrap(entryBundle.getParcelable("OBJECT"));
      Entry entry = new Entry(object);
      entry.viewState = entryBundle.getSparseParcelableArray("VIEW_STATE");
      entries.add(entry);
    }
    return new History(entries);
  }

  private final Deque<Entry> history;

  /** Get a {@link Parcelable} of this history using the supplied {@link StateParceler}. */
  public Parcelable getParcelable(StateParceler parceler) {
    Bundle historyBundle = new Bundle();
    ArrayList<Bundle> entryBundles = new ArrayList<>(history.size());
    for (Entry entry : history) {
     entryBundles.add(entry.getBundle(parceler));
    }
    historyBundle.putParcelableArrayList("ENTRIES", entryBundles);
    return historyBundle;
  }

  /**
   * Get a {@link Parcelable} of this history using the supplied {@link StateParceler}, filtered
   * by the supplied {@link Filter}.
   *
   * The filter is invoked on each state in the stack in reverse order
   *
   * @return null if all states are filtered out.
   */
  public Parcelable getParcelable(StateParceler parceler, Filter filter) {
    Bundle historyBundle = new Bundle();
    ArrayList<Bundle> entryBundles = new ArrayList<>(history.size());
    Iterator<Entry> it = history.descendingIterator();
    while (it.hasNext()) {
      Entry entry = it.next();
      if (filter.apply(entry.state)) {
        entryBundles.add(entry.getBundle(parceler));
      }
    }
    if (entryBundles.isEmpty()) {
      return null;
    }
    Collections.reverse(entryBundles);
    historyBundle.putParcelableArrayList("ENTRIES", entryBundles);
    return historyBundle;
  }

  public static Builder emptyBuilder() {
    return new Builder(Collections.<Entry>emptyList());
  }

  /** Create a history that contains a single object. */
  public static History single(Object object) {
    return emptyBuilder().push(object).build();
  }

  private History(Deque<Entry> history) {
    checkArgument(history != null && !history.isEmpty(), "History may not be empty");
    this.history = history;
  }

  @SuppressWarnings("UnusedDeclaration")
  @Override public Iterator<Object> iterator() {
    return new ReadIterator<>(history.iterator());
  }

  public <T> Iterator<T> reverseIterator() {
    return new ReadIterator<>(history.descendingIterator());
  }

  public int size() {
    return history.size();
  }

  public <T> T top() {
    //noinspection unchecked
    return (T) history.peek().state;
  }

  public ViewState currentViewState() {
    return history.peek();
  }

  /**
   * Get a builder to modify a copy of this history.
   *
   * The builder returned will retain all internal information related to the states in the history,
   * including any associated View state. It is safe to remove states from the builder and push
   * them back on; nothing will be lost in those operations.
   * */
  public Builder buildUpon() {
    return new Builder(history);
  }

  @Override public String toString() {
    return history.toString();
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
    private final Deque<Entry> history;
    private final Map<Object, Entry> entryMemory = new LinkedHashMap<>();

    private Builder(Collection<Entry> history) {
      this.history = new ArrayDeque<>(history);
    }

    public Builder clear() {
      while (!history.isEmpty()) {
        pop();
      }
      return this;
    }

    public Builder push(Object object) {
      Entry entry = entryMemory.get(object);
      if (entry == null) {
        entry = new Entry(object);
      }
      history.push(entry);
      entryMemory.remove(object);
      return this;
    }

    public Builder addAll(Collection<Object> c) {
      for (Object object : c) {
        push(object);
      }

      return this;
    }

    public Object peek() {
      final Entry peek = history.peek();
      return peek == null ? null : peek.state;
    }

    public boolean isEmpty() {
      return peek() == null;
    }

    /** @throws IllegalStateException if empty */
    public Object pop() {
      if (isEmpty()) {
        throw new IllegalStateException("Cannot pop from an empty builder");
      }
      Entry entry = history.pop();
      entryMemory.put(entry.state, entry);
      return entry.state;
    }

    public History build() {
      return new History(history);
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

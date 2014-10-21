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
import java.util.LinkedList;

/**
 * Describes the history of a {@link Flow} at a specific point in time. For persisting the supplied
 * {@link Parceler} needs to be able to handle all screen types.
 */
public final class Backstack implements Iterable<Backstack.Entry> {
  private final long highestId;
  private final Deque<Entry> backstack;

  /** Restore a saved backstack from a {@link Parcelable} using the supplied {@link Parceler}. */
  public static Backstack from(Parcelable parcelable, Parceler<Object> parceler) {
    Bundle bundle = (Bundle) parcelable; // TODO: assert/throw
    ArrayList<Bundle> entryBundles = bundle.getParcelableArrayList("ENTRIES");
    Deque<Entry> entries = new ArrayDeque<>(entryBundles.size());
    for (Bundle entryBundle : entryBundles) {
      Object path = parceler.unwrap(entryBundle.getParcelable("PATH"));
      Entry entry = new Entry(path);
      entry.viewState = entryBundle.getSparseParcelableArray("VIEW_STATE");
      entries.add(entry);
    }
    return new Backstack(0, entries);
  }

  /** Get a {@link Parcelable} of this backstack using the supplied {@link Parceler}. */
  public Parcelable getParcelable(Parceler<Object> parceler) {
    Bundle backstackBundle = new Bundle();

    ArrayList<Bundle> entryBundles = new ArrayList<>(backstack.size());

    for (Entry entry : backstack) {
      Bundle entryBundle = new Bundle();
      entryBundle.putParcelable("PATH", parceler.wrap(entry.path));
      entryBundle.putSparseParcelableArray("VIEW_STATE", entry.viewState);
      entryBundles.add(entryBundle);
    }
    backstackBundle.putParcelableArrayList("ENTRIES", entryBundles);
    return backstackBundle;
  }

  public static Builder emptyBuilder() {
    return new Builder(-1, Collections.<Entry>emptyList());
  }

  /** Create a backstack that contains a single screen. */
  public static Backstack single(Object screen) {
    return emptyBuilder().push(screen).build();
  }

  private Backstack(long highestId, Deque<Entry> backstack) {
    this.highestId = highestId;
    this.backstack = backstack;
  }

  @Override public Iterator<Entry> iterator() {
    return new ReadIterator<Entry>(backstack.iterator());
  }

  public Iterator<Entry> reverseIterator() {
    return new ReadIterator<Entry>(backstack.descendingIterator());
  }


  public int size() {
    return backstack.size();
  }

  public Entry current() {
    return backstack.peek();
  }

  /** Get a builder to modify a copy of this backstack. */
  public Builder buildUpon() {
    return new Builder(highestId, backstack);
  }

  @Override public String toString() {
    return backstack.toString();
  }

  public static Backstack fromUpChain(Object screen) {
    LinkedList<Object> newBackstack = new LinkedList<Object>();

    Object current = screen;
    while (current instanceof HasParent<?>) {
      newBackstack.addFirst(current);
      current = ((HasParent) current).getParent();
    }
    newBackstack.addFirst(current);

    Backstack.Builder builder = emptyBuilder();
    builder.addAll(newBackstack);
    return builder.build();
  }

  public static final class Entry {
    public final Object path;
    SparseArray<Parcelable> viewState;

    private Entry(Object path) {
      this.path = path;
    }

    public Object getPath() {
      return path;
    }

    public void saveViewState(View view) {
      SparseArray<Parcelable> state = new SparseArray<>();
      view.saveHierarchyState(state);
      viewState = state;
    }

    public void restoreViewState(View view) {
      if (viewState != null) {
        view.restoreHierarchyState(viewState);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Entry entry = (Entry) o;
      return (path.equals(entry.path));
    }

    @Override
    public int hashCode() {
      return path.hashCode();
    }
  }

  public static final class Builder {
    private long highestId;
    private final Deque<Entry> backstack;

    private Builder(long highestId, Collection<Entry> backstack) {
      this.highestId = highestId;
      this.backstack = new ArrayDeque<Entry>(backstack);
    }

    public Builder push(Object screen) {
      backstack.push(new Entry(screen));

      return this;
    }

    public Builder addAll(Collection<Object> c) {
      for (Object screen : c) {
        backstack.push(new Entry(screen));
      }

      return this;
    }

    public Entry peek() {
      return backstack.peek();
    }

    public Entry pop() {
      return backstack.pop();
    }

    public Builder clear() {
      backstack.clear();

      return this;
    }

    public Backstack build() {
      if (backstack.isEmpty()) {
        throw new IllegalStateException("Backstack may not be empty");
      }

      return new Backstack(highestId, backstack);
    }
  }

  private static class ReadIterator<T> implements Iterator<T> {
    private final Iterator<T> iterator;

    public ReadIterator(Iterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override public T next() {
      return iterator.next();
    }

    @Override public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}

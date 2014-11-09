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
 * Describes the history of a {@link Flow} at a specific point in time.
 */
public final class Backstack implements Iterable<Path> {
  private final Deque<Entry> backstack;

  /** Restore a saved backstack from a {@link Parcelable} using the supplied {@link Parceler}. */
  public static Backstack from(Parcelable parcelable, Parceler parceler) {
    Bundle bundle = (Bundle) parcelable; // TODO(loganj): assert/throw
    ArrayList<Bundle> entryBundles = bundle.getParcelableArrayList("ENTRIES");
    Deque<Entry> entries = new ArrayDeque<>(entryBundles.size());
    for (Bundle entryBundle : entryBundles) {
      Path path = parceler.unwrap(entryBundle.getParcelable("PATH"));
      Entry entry = new Entry(path);
      entry.viewState = entryBundle.getSparseParcelableArray("VIEW_STATE");
      entries.add(entry);
    }
    return new Backstack(entries);
  }

  /** Get a {@link Parcelable} of this backstack using the supplied {@link Parceler}. */
  public Parcelable getParcelable(Parceler parceler) {
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
    return new Builder(Collections.<Entry>emptyList());
  }

  /** Create a backstack that contains a single path. */
  public static Backstack single(Path path) {
    return emptyBuilder().push(path).build();
  }

  private Backstack(Deque<Entry> backstack) {
    this.backstack = backstack;
  }

  @Override public Iterator<Path> iterator() {
    return new ReadIterator(backstack.iterator());
  }

  public Iterator<Path> reverseIterator() {
    return new ReadIterator(backstack.descendingIterator());
  }


  public int size() {
    return backstack.size();
  }

  public Path current() {
    return currentEntry().getPath();
  }

  Entry currentEntry() {
    return backstack.peek();
  }

  /** Get a builder to modify a copy of this backstack. */
  public Builder buildUpon() {
    return new Builder(backstack);
  }

  @Override public String toString() {
    return backstack.toString();
  }

  public static Backstack fromUpChain(Path path) {
    LinkedList<Path> newBackstack = new LinkedList<>();

    Path current = path;
    while (current instanceof HasParent) {
      newBackstack.addFirst(current);
      current = ((HasParent) current).getParent();
    }
    newBackstack.addFirst(current);

    Backstack.Builder builder = emptyBuilder();
    builder.addAll(newBackstack);
    return builder.build();
  }

  static final class Entry {
    public final Path path;
    SparseArray<Parcelable> viewState;

    private Entry(Path path) {
      this.path = path;
    }

    public Path getPath() {
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
    private final Deque<Entry> backstack;

    private Builder(Collection<Entry> backstack) {
      this.backstack = new ArrayDeque<>(backstack);
    }

    public Builder push(Path path) {
      Entry entry = new Entry(path);
      backstack.push(entry);

      return this;
    }

    public Builder addAll(Collection<Path> c) {
      for (Path path : c) {
        backstack.push(new Entry(path));
      }

      return this;
    }

    public Path peek() {
      return backstack.peek().getPath();
    }

    public Path pop() {
      return backstack.pop().getPath();
    }

    public Backstack build() {
      if (backstack.isEmpty()) {
        throw new IllegalStateException("Backstack may not be empty");
      }

      return new Backstack(backstack);
    }
  }

  private static class ReadIterator implements Iterator<Path> {
    private final Iterator<Entry> iterator;

    public ReadIterator(Iterator<Entry> iterator) {
      this.iterator = iterator;
    }

    @Override public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override public Path next() {
      return iterator.next().getPath();
    }

    @Override public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import static flow.Preconditions.checkArgument;

/**
 * Describes the history of a {@link Flow} at a specific point in time.
 */
public final class History implements Iterable<Object> {
  public interface Filter {
    boolean apply(Object key);
  }

  /** Restore a saved history from a {@link Parcelable} using the supplied {@link KeyParceler}. */
  public static History from(Parcelable parcelable, KeyParceler parceler) {
    Bundle bundle = (Bundle) parcelable; // TODO(loganj): assert/throw
    ArrayList<Bundle> entryBundles = bundle.getParcelableArrayList("ENTRIES");
    if (entryBundles == null) throw new AssertionError("Parcelable does not contain history");
    List<State> entries = new ArrayList<>(entryBundles.size());
    for (Bundle entryBundle : entryBundles) {
      Object object = parceler.toKey(entryBundle.getParcelable("OBJECT"));
      final Object key = object;
      State entry = new State(key);
      entry.viewState = entryBundle.getSparseParcelableArray("VIEW_STATE");
      entries.add(entry);
    }
    return new History(entries);
  }

  private final List<State> history;

  /** Get a {@link Parcelable} of this history using the supplied {@link KeyParceler}. */
  public Parcelable getParcelable(KeyParceler parceler) {
    Bundle historyBundle = new Bundle();
    ArrayList<Bundle> entryBundles = new ArrayList<>(history.size());
    for (State entry : history) {
      entryBundles.add(entry.toBundle(parceler));
    }
    historyBundle.putParcelableArrayList("ENTRIES", entryBundles);
    return historyBundle;
  }

  /**
   * Get a {@link Parcelable} of this history using the supplied {@link KeyParceler}, filtered
   * by the supplied {@link Filter}.
   *
   * The filter is invoked on each key in the stack in reverse order
   *
   * @return null if all keys are filtered out.
   */
  public Parcelable getParcelable(KeyParceler parceler, Filter filter) {
    Bundle historyBundle = new Bundle();
    ArrayList<Bundle> entryBundles = new ArrayList<>(history.size());
    ListIterator<State> it = history.listIterator();
    while (it.hasPrevious()) {
      State entry = it.previous();
      if (filter.apply(entry.getKey())) {
        entryBundles.add(entry.toBundle(parceler));
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
    return new Builder(Collections.<State>emptyList());
  }

  /** Create a history that contains a single key. */
  public static History single(Object key) {
    return emptyBuilder().push(key).build();
  }

  private History(List<State> history) {
    checkArgument(history != null && !history.isEmpty(), "History may not be empty");
    this.history = history;
  }

  public <T> Iterator<T> reverseIterator() {
    return new ReadStateIterator<>(history.iterator());
  }

  @SuppressWarnings("UnusedDeclaration") @Override public Iterator<Object> iterator() {
    return new ReadStateIterator<>(new ReverseIterator<>(history));
  }

  public int size() {
    return history.size();
  }

  public <T> T top() {
    //noinspection unchecked
    return (T) peekSaveState(0).getKey();
  }

  /** Returns the app state at the provided index in history. 0 is the newest entry. */
  public <T> T peek(int index) {
    //noinspection unchecked
    return (T) peekSaveState(index).getKey();
  }

  /**
   * Returns the {@link State} at the provided index in history. 0 is the newest entry.
   */
  public State peekSaveState(int index) {
    return history.get(history.size() - index - 1);
  }

  public State topSaveState() {
    return peekSaveState(0);
  }

  /**
   * Get a builder to modify a copy of this history.
   * <p>
   * The builder returned will retain all internal information related to the keys in the
   * history, including their states. It is safe to remove keys from the builder and push them back
   * on; nothing will be lost in those operations.
   */
  public Builder buildUpon() {
    return new Builder(history);
  }

  @Override public String toString() {
    return Arrays.deepToString(history.toArray());
  }

  public static final class Builder {
    private final List<State> history;
    private final Map<Object, State> entryMemory = new LinkedHashMap<>();

    private Builder(Collection<State> history) {
      this.history = new ArrayList<>(history);
    }

    /**
     * Removes all keys from this builder. But note that if this builder was created
     * via {@link #buildUpon()}, any state associated with the cleared
     * keys will be preserved and will be restored if they are {@link #push pushed}
     * back on.
     */
    public Builder clear() {
      // Clear by popping everything (rather than just calling history.clear()) to
      // fill up entryMemory. Otherwise we drop view state on the floor.
      while (!isEmpty()) {
        pop();
      }

      return this;
    }

    /**
     * Adds a key to the builder. If this builder was created via {@link #buildUpon()},
     * and the pushed key was previously {@link #pop() popped} or {@link #clear cleared}
     * from the builder, the key's associated state will be restored.
     */
    public Builder push(Object key) {
      State entry = entryMemory.get(key);
      if (entry == null) {
        final Object key1 = key;
        entry = new State(key1);
      }
      history.add(entry);
      entryMemory.remove(key);
      return this;
    }

    /**
     * {@link #push Pushes} all of the keys in the collection onto this builder.
     */
    public Builder addAll(Collection<?> c) {
      for (Object key : c) {
        push(key);
      }
      return this;
    }

    public Object peek() {
      return history.isEmpty() ? null : history.get(history.size() - 1).getKey();
    }

    public boolean isEmpty() {
      return history.isEmpty();
    }

    /**
     * Removes the last state added. Note that if this builder was created
     * via {@link #buildUpon()}, any view state associated with the popped
     * state will be preserved, and restored if it is {@link #push pushed}
     * back in.
     *
     * @throws IllegalStateException if empty
     */
    public Object pop() {
      if (isEmpty()) {
        throw new IllegalStateException("Cannot pop from an empty builder");
      }
      State entry = history.remove(history.size() - 1);
      entryMemory.put(entry.getKey(), entry);
      return entry.getKey();
    }

    /**
     * Pops the history until the given state is at the top.
     *
     * @throws IllegalArgumentException if the given state isn't in the history.
     */
    public Builder popTo(Object state) {
      //noinspection ConstantConditions
      while (!isEmpty() && !peek().equals(state)) {
        pop();
      }
      checkArgument(!isEmpty(), String.format("%s not found in history", state));
      return this;
    }

    public Builder pop(int count) {
      final int size = history.size();
      checkArgument(count <= size,
          String.format((Locale) null, "Cannot pop %d elements, history only has %d", count, size));
      while (count-- > 0) {
        pop();
      }
      return this;
    }

    public History build() {
      return new History(history);
    }

    @Override public String toString() {
      return Arrays.deepToString(history.toArray());
    }
  }

  private static class ReverseIterator<T> implements Iterator<T> {
    private final ListIterator<T> wrapped;

    ReverseIterator(List<T> list) {
      wrapped = list.listIterator(list.size());
    }

    @Override public boolean hasNext() {
      return wrapped.hasPrevious();
    }

    @Override public T next() {
      return wrapped.previous();
    }

    @Override public void remove() {
      wrapped.remove();
    }
  }

  private static class ReadStateIterator<T> implements Iterator<T> {
    private final Iterator<State> iterator;

    ReadStateIterator(Iterator<State> iterator) {
      this.iterator = iterator;
    }

    @Override public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override public T next() {
      //noinspection unchecked
      return (T) iterator.next().getKey();
    }

    @Override public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}

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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import static flow.Preconditions.checkArgument;
import static java.util.Collections.unmodifiableList;

/**
 * Describes the history of a {@link Flow} at a specific point in time.
 */
public final class History implements Iterable<Object> {

  private final List<Object> history;

  @NonNull public static Builder emptyBuilder() {
    return new Builder(Collections.emptyList());
  }

  /** Create a history that contains a single key. */
  @NonNull public static History single(@NonNull Object key) {
    return emptyBuilder().push(key).build();
  }

  private History(List<Object> history) {
    checkArgument(history != null && !history.isEmpty(), "History may not be empty");
    this.history = history;
  }

  @NonNull public <T> Iterator<T> reverseIterator() {
    return new ReadStateIterator<>(history.iterator());
  }

  @NonNull @Override public Iterator<Object> iterator() {
    return new ReadStateIterator<>(new ReverseIterator<>(history));
  }

  public int size() {
    return history.size();
  }

  @NonNull public <T> T top() {
    return peek(0);
  }

  /** Returns the app state at the provided index in history. 0 is the newest entry. */
  @NonNull public <T> T peek(int index) {
    //noinspection unchecked
    return (T) history.get(history.size() - index - 1);
  }

  @NonNull List<Object> asList() {
    final ArrayList<Object> copy = new ArrayList<>(history);
    return unmodifiableList(copy);
  }

  /**
   * Get a builder to modify a copy of this history.
   * <p>
   * The builder returned will retain all internal information related to the keys in the
   * history, including their states. It is safe to remove keys from the builder and push them back
   * on; nothing will be lost in those operations.
   */
  @NonNull public Builder buildUpon() {
    return new Builder(history);
  }

  @Override public String toString() {
    return Arrays.deepToString(history.toArray());
  }

  public static final class Builder {
    private final List<Object> history;

    private Builder(Collection<Object> history) {
      this.history = new ArrayList<>(history);
    }

    /**
     * Removes all keys from this builder. But note that if this builder was created
     * via {@link #buildUpon()}, any state associated with the cleared
     * keys will be preserved and will be restored if they are {@link #push pushed}
     * back on.
     */
    @NonNull public Builder clear() {
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
    @NonNull public Builder push(@NonNull Object key) {
      history.add(key);
      return this;
    }

    /**
     * {@link #push Pushes} all of the keys in the collection onto this builder.
     */
    @NonNull public Builder pushAll(@NonNull Collection<?> c) {
      for (Object key : c) {
        //noinspection CheckResult
        push(key);
      }
      return this;
    }

    /** @return null if the history is empty. */
    @Nullable public Object peek() {
      return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    @NonNull public boolean isEmpty() {
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
      return history.remove(history.size() - 1);
    }

    /**
     * Pops the history until the given state is at the top.
     *
     * @throws IllegalArgumentException if the given state isn't in the history.
     */
    @NonNull public Builder popTo(@NonNull Object state) {
      //noinspection ConstantConditions
      while (!isEmpty() && !peek().equals(state)) {
        pop();
      }
      checkArgument(!isEmpty(), String.format("%s not found in history", state));
      return this;
    }

    @NonNull public Builder pop(int count) {
      final int size = history.size();
      checkArgument(count <= size,
          String.format((Locale) null, "Cannot pop %d elements, history only has %d", count, size));
      while (count-- > 0) {
        pop();
      }
      return this;
    }

    @NonNull public History build() {
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
    private final Iterator<Object> iterator;

    ReadStateIterator(Iterator<Object> iterator) {
      this.iterator = iterator;
    }

    @Override public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override public T next() {
      //noinspection unchecked
      return (T) iterator.next();
    }

    @Override public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}

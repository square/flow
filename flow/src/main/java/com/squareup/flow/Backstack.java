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

package com.squareup.flow;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/** Describes the backstack of a flow at a specific point in time. */
public final class Backstack implements Iterable<Backstack.Entry> {
  private final long highestId;
  private final Deque<Entry> backstack;

  public static Backstack from(Parcelable parcelable, Parcer<Screen> parcer) {
    ParcelableBackstack backstack = (ParcelableBackstack) parcelable;
    return backstack.getBackstack(parcer);
  }

  public static Builder emptyBuilder() {
    return new Builder(-1, Collections.<Entry>emptyList());
  }

  public static Backstack single(Screen screen) {
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

  public Parcelable getParcelable(Parcer<Screen> parcer) {
    return new ParcelableBackstack.Memory(this, parcer);
  }

  public int size() {
    return backstack.size();
  }

  public Entry current() {
    return backstack.peek();
  }

  public Builder buildUpon() {
    return new Builder(highestId, backstack);
  }

  @Override public String toString() {
    return backstack.toString();
  }

  public static class Entry {
    private final long id;
    private final Screen screen;

    private Entry(long id, Screen screen) {
      this.id = id;
      this.screen = screen;
    }

    public long getId() {
      return id;
    }

    public Screen getScreen() {
      return screen;
    }

    @Override public String toString() {
      return "{" + id + ", " + screen + "}";
    }
  }

  public static class Builder {
    private long highestId;
    private final Deque<Entry> backstack;

    private Builder(long highestId, Collection<Entry> backstack) {
      this.highestId = highestId;
      this.backstack = new ArrayDeque<Entry>(backstack);
    }

    public Builder push(Screen screen) {
      backstack.push(new Entry(++highestId, screen));

      return this;
    }

    public Builder addAll(Collection<Screen> c) {
      for (Screen screen : c) {
        backstack.push(new Entry(++highestId, screen));
      }

      return this;
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

  private interface ParcelableBackstack extends Parcelable {
    Backstack getBackstack(Parcer<Screen> parcer);

    public static final Parcelable.Creator<ParcelableBackstack> CREATOR =
        new Parcelable.Creator<ParcelableBackstack>() {
          @Override public ParcelableBackstack createFromParcel(Parcel in) {
            Parcelable[] parcelables = in.readParcelableArray(getClass().getClassLoader());
            return new Parcelled(parcelables);
          }

          @Override public ParcelableBackstack[] newArray(int size) {
            return new Parcelled[size];
          }
        };

    static class Memory implements ParcelableBackstack {
      private final Backstack backstack;
      private final Parcer<Screen> parcer;

      Memory(Backstack backstack, Parcer<Screen> parcer) {
        this.backstack = backstack;
        this.parcer = parcer;
      }

      @Override public Backstack getBackstack(Parcer<Screen> parcer) {
        return backstack;
      }

      @Override public int describeContents() {
        return 0;
      }

      @Override public void writeToParcel(Parcel out, int flags) {
        Parcelable[] parcelables = new Parcelable[backstack.size()];
        int i = 0;
        for (Iterator<Entry> iterator = backstack.reverseIterator(); iterator.hasNext(); ) {
          Entry entry = iterator.next();
          parcelables[i++] = parcer.wrap(entry.getScreen());
        }

        out.writeParcelableArray(parcelables, flags);
      }
    }

    static class Parcelled implements ParcelableBackstack {
      private final Parcelable[] parcelables;

      public Parcelled(Parcelable[] parcelables) {
        this.parcelables = parcelables;
      }

      @Override public Backstack getBackstack(Parcer<Screen> parcer) {
        // TODO: This does not preserve the backstack id
        List<Screen> screens = new ArrayList<Screen>();
        for (Parcelable parcelable : parcelables) {
          screens.add(parcer.unwrap(parcelable));
        }

        return emptyBuilder().addAll(screens).build();
      }

      @Override public int describeContents() {
        return 0;
      }

      @Override public void writeToParcel(Parcel out, int flags) {
        out.writeParcelableArray(parcelables, flags);
      }
    }
  }
}

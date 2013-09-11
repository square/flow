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
public final class Backstack implements Iterable<Backstack.Entry>, Parcelable {
  private final long highestId;
  private final Deque<Entry> backstack;

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

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel out, int flags) {
    out.writeLong(highestId);
    List<Entry> list = new ArrayList<Entry>(backstack);
    out.writeTypedList(list);
  }

  public static final Parcelable.Creator<Backstack> CREATOR = new Parcelable.Creator<Backstack>() {
    @Override public Backstack createFromParcel(Parcel in) {
      long highestId = in.readLong();
      List<Entry> list = new ArrayList<Entry>();
      in.readTypedList(list, Entry.CREATOR);
      Deque<Entry> backstack = new ArrayDeque<Entry>(list);
      return new Backstack(highestId, backstack);
    }

    @Override public Backstack[] newArray(int size) {
      return new Backstack[size];
    }
  };

  public static class Entry implements Parcelable {
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

    @Override public int describeContents() {
      return 0;
    }

    @Override public void writeToParcel(Parcel out, int flags) {
      out.writeLong(id);
      out.writeParcelable(screen, screen.describeContents());
    }

    public static final Parcelable.Creator<Entry> CREATOR = new Parcelable.Creator<Entry>() {
      @Override public Entry createFromParcel(Parcel in) {
        long id = in.readLong();
        Screen screen = in.readParcelable(getClass().getClassLoader());

        return new Entry(id, screen);
      }

      @Override public Entry[] newArray(int size) {
        return new Entry[size];
      }
    };
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
}

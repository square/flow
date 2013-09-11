package com.squareup.flow;

import java.util.Iterator;
import java.util.LinkedList;

public final class Flow {
  public enum Direction {
    FORWARD, BACKWARD
  }

  public interface Listener {
    void go(Backstack backstack, Direction direction);
  }

  private final Listener listener;
  private Backstack backstack;

  public Flow(Backstack backstack, Listener listener) {
    this.listener = listener;
    this.backstack = backstack;
  }

  public Backstack getBackstack() {
    return backstack;
  }

  public void goTo(Screen desired) {
    Backstack newBackstack = backstack.buildUpon().push(desired).build();
    forward(newBackstack);
  }

  public void resetTo(Screen screen) {
    Backstack.Builder builder = backstack.buildUpon();
    int count = 0;
    for (Iterator<Backstack.Entry> it = backstack.reverseIterator(); it.hasNext(); ) {
      Backstack.Entry entry = it.next();

      if (entry.getScreen() == screen) {
        // Clear up to the target screen.
        for (int i = 0; i < backstack.size() - count; i++) {
          builder.pop();
        }
        break;
      } else {
        count++;
      }
    }

    builder.push(screen);
    backward(builder.build());
  }

  public void replaceTo(Screen to) {
    LinkedList<Screen> newBackstack = new LinkedList<Screen>();

    Screen current = to;
    while (current instanceof Screen.HasParent<?>) {
      newBackstack.addFirst(current);
      current = ((Screen.HasParent) current).getParent();
    }
    newBackstack.addFirst(current);

    Backstack.Builder builder = backstack.buildUpon().clear();
    builder.addAll(newBackstack);

    backward(builder.build());
  }

  public boolean goUp() {
    Screen current = backstack.current().getScreen();
    if (current instanceof Screen.HasParent<?>) {
      replaceTo(((Screen.HasParent) current).getParent());
      return true;
    } else {
      return false;
    }
  }

  public boolean goBack() {
    if (backstack.size() == 1) {
      return false;
    }

    Backstack.Builder builder = backstack.buildUpon();
    builder.pop();
    backward(builder.build());

    return true;
  }

  public void forward(Backstack newBackstack) {
    listener.go(newBackstack, Direction.FORWARD);
    backstack = newBackstack;
  }

  public void backward(Backstack newBackstack) {
    listener.go(newBackstack, Direction.BACKWARD);
    backstack = newBackstack;
  }
}

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

import java.util.Iterator;
import java.util.LinkedList;

/** Holds the current truth, the history of screens, and exposes operations to change it. */
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

  /** Push the screen onto the backstack. */
  public void goTo(Object screen) {
    Backstack newBackstack = backstack.buildUpon().push(screen).build();
    forward(newBackstack);
  }

  /** Reset to the specified screen. Pops until the screen is found (inclusive) and appends.  */
  public void resetTo(Object screen) {
    Backstack.Builder builder = backstack.buildUpon();
    int count = 0;
    for (Iterator<Backstack.Entry> it = backstack.reverseIterator(); it.hasNext();) {
      Backstack.Entry entry = it.next();

      if (entry.getScreen().equals(screen)) {
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

  /** Replaces the current backstack with the up stack of the screen. */
  public void replaceTo(Object screen) {
    LinkedList<Object> newBackstack = new LinkedList<Object>();

    Object current = screen;
    while (current instanceof HasParent<?>) {
      newBackstack.addFirst(current);
      current = ((HasParent) current).getParent();
    }
    newBackstack.addFirst(current);

    Backstack.Builder builder = backstack.buildUpon().clear();
    builder.addAll(newBackstack);

    backward(builder.build());
  }

  /**
   * Go up one screen.
   * @return false if going up is not possible.
   */
  public boolean goUp() {
    Object current = backstack.current().getScreen();
    if (current instanceof HasParent<?>) {
      replaceTo(((HasParent) current).getParent());
      return true;
    } else {
      return false;
    }
  }

  /**
   * Go back one screen.
   * @return false if going back is not possible.
   */
  public boolean goBack() {
    if (backstack.size() == 1) {
      return false;
    }

    Backstack.Builder builder = backstack.buildUpon();
    builder.pop();
    backward(builder.build());

    return true;
  }

  /** Goes forward to a new backstack. */
  public void forward(Backstack newBackstack) {
    listener.go(newBackstack, Direction.FORWARD);
    backstack = newBackstack;
  }

  /** Goes backward to a new backstack. */
  public void backward(Backstack newBackstack) {
    listener.go(newBackstack, Direction.BACKWARD);
    backstack = newBackstack;
  }
}

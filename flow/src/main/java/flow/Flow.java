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

/** Holds the current truth, the history of screens, and exposes operations to change it. */
public final class Flow {
  public enum Direction {
    FORWARD, BACKWARD, REPLACE
  }

  public interface Listener {
    void go(Backstack backstack, Direction direction);
  }

  private final Listener listener;
  private Backstack backstack;
  private Backstack nextBackstack;

  public Flow(Backstack backstack, Listener listener) {
    this.listener = listener;
    this.backstack = backstack;
  }

  public Backstack getBackstack() {
    return backstack;
  }

  /**
   * During transitions (that is, during calls to {@link Listener#go}), returns the
   * backstack we're transitioning to. At any other time is equivalent to {@link #getBackstack()}.
   */
  public Backstack getNextBackstack() {
    return nextBackstack != null ? nextBackstack : getBackstack();
  }

  /** Push the screen onto the backstack. */
  public void goTo(Object screen) {
    Backstack newBackstack = backstack.buildUpon().push(screen).build();
    forward(newBackstack);
  }

  /**
   * Reset to the specified screen. Pops until the screen is found.  If the screen is not found,
   * the entire backstack is replaced with the screen.
   */
  public void resetTo(Object screen) {
    Backstack.Builder builder = backstack.buildUpon();
    int count = 0;
    // Take care to leave the original screen instance on the stack, if we find it.  This enables
    // some arguably bad behavior on the part of clients, but it's still probably the right thing
    // to do.
    Object lastPopped = null;
    for (Iterator<Backstack.Entry> it = backstack.reverseIterator(); it.hasNext();) {
      Backstack.Entry entry = it.next();

      if (entry.getScreen().equals(screen)) {
        // Clear up to the target screen.
        for (int i = 0; i < backstack.size() - count; i++) {
          lastPopped = builder.pop().getScreen();
        }
        break;
      } else {
        count++;
      }
    }

    if (lastPopped != null) {
      builder.push(lastPopped);
      backward(builder.build());
    } else {
      builder.push(screen);
      forward(builder.build());
    }
  }

  /** Replaces the current backstack with the up stack of the screen. */
  public void replaceTo(Object screen) {
    replace(preserveEquivalentPrefix(backstack, Backstack.fromUpChain(screen)));
  }

  /**
   * Go up one screen.
   *
   * @return false if going up is not possible.
   */
  public boolean goUp() {
    Object current = backstack.current().getScreen();
    if (current instanceof HasParent<?>) {
      Object parent = ((HasParent) current).getParent();
      backward(preserveEquivalentPrefix(backstack, Backstack.fromUpChain(parent)));
      return true;
    } else {
      return false;
    }
  }

  /**
   * Go back one screen.
   *
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
    go(newBackstack, Direction.FORWARD);
  }

  /** Goes backward to a new backstack. */
  public void backward(Backstack newBackstack) {
    go(newBackstack, Direction.BACKWARD);
  }

  /** Replaces to a new backstack. */
  private void replace(Backstack newBackstack) {
    go(newBackstack, Direction.REPLACE);
  }

  private void go(Backstack newBackstack, Direction direction) {
    //if (nextBackstack != null) {
    //  throw new IllegalStateException(
    //      "Cannot transition while transitioning, already going to " + nextBackstack);
    //}
    nextBackstack = newBackstack;
    listener.go(newBackstack, direction);
    backstack = newBackstack;
    nextBackstack = null;
  }

  private static Backstack preserveEquivalentPrefix(Backstack current, Backstack proposed) {
    Iterator<Backstack.Entry> oldIt = current.reverseIterator();
    Iterator<Backstack.Entry> newIt = proposed.reverseIterator();

    Backstack.Builder preserving = Backstack.emptyBuilder();

    while (newIt.hasNext()) {
      Backstack.Entry newEntry = newIt.next();
      if (!oldIt.hasNext()) {
        preserving.push(newEntry.getScreen());
        break;
      }
      Backstack.Entry oldEntry = oldIt.next();
      if (oldEntry.getScreen().equals(newEntry.getScreen())) {
        preserving.push(oldEntry.getScreen());
      } else {
        preserving.push(newEntry.getScreen());
        break;
      }
    }

    while (newIt.hasNext()) {
      preserving.push(newIt.next().getScreen());
    }
    return preserving.build();
  }
}

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

  /** Supplied by Flow to the Listener, which is responsible for calling onComplete(). */
  public interface Callback {
    /**
     * Must be called exactly once to indicate that the corresponding transition has completed.
     *
     * If not called, the backstack will not be updated and further calls to Flow will not execute.
     * Calling more than once will result in an exception.
     */
    void onComplete();
  }

  public interface Listener {
    /**
     * Notifies the listener that the backstack is about to change. Note that the backstack of
     * is not actually changed until the callback is triggered.  That is, {@code nextBackstack} is
     * where the Flow is going next, and {@link Flow#getBackstack()} is where it's coming from.
     *
     * @param callback Must be called to indicate completion.
     */
    void go(Backstack nextBackstack, Direction direction, Callback callback);
  }

  private final Listener listener;
  private Backstack backstack;
  private Transition transition;

  public Flow(Backstack backstack, Listener listener) {
    this.listener = listener;
    this.backstack = backstack;
  }

  public Backstack getBackstack() {
    return backstack;
  }

  /** Push the screen onto the backstack. */
  public void goTo(final Object screen) {
    move(new Transition() {
      @Override public void execute() {
        Backstack newBackstack = backstack.buildUpon().push(screen).build();
        go(newBackstack, Direction.FORWARD);
      }
    });
  }

  /**
   * Reset to the specified screen. Pops until the screen is found.  If the screen is not found,
   * the entire backstack is replaced with the screen.
   */
  public void resetBackTo(final Object screen) {
    move(new Transition() {
      @Override public void execute() {
        Backstack.Builder builder = backstack.buildUpon();
        int count = 0;
        // Take care to leave the original screen instance on the stack, if we find it.  This enables
        // some arguably bad behavior on the part of clients, but it's still probably the right thing
        // to do.
        Object lastPopped = null;
        for (Iterator<Backstack.Entry> it = backstack.reverseIterator(); it.hasNext(); ) {
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

        Backstack newBackstack;
        if (lastPopped != null) {
          builder.push(lastPopped);
          newBackstack = builder.build();
          go(newBackstack, Direction.BACKWARD);
        } else {
          builder.push(screen);
          newBackstack = builder.build();
          go(newBackstack, Direction.FORWARD);
        }
      }
    });
  }

  /** Replaces the current backstack with the up stack of the screen. */
  public void resetToUpStackOf(final Object screen, final Direction direction) {
    move(new Transition() {
      @Override public void execute() {
        Backstack newBackstack = preserveEquivalentPrefix(backstack, Backstack.fromUpChain(screen));
        go(newBackstack, direction);
      }
    });
  }

  /**
   * Replaces the backstack with one containing only the given screen, reported to the
   * listener with the given direction.
   */
  public void resetTo(Object screen, Direction direction) {
    resetTo(Backstack.single(screen), direction);
  }

  /**
   * Replaces the backstack with given one, reported to the listener with the given direction.
   */
  public void resetTo(final Backstack newBackstack, final Direction direction) {
    move(new Transition() {
      @Override public void execute() {
        // TODO(rjrjr,loganj): Should this be using preserveEquivalentPrefix? Or should go?
        go(newBackstack, direction);
      }
    });
  }

  /**
   * Go up one screen.
   *
   * @return false if going up is not possible.
   */
  public boolean goUp() {
    boolean canGoUp = false;
    if (backstack.current().getScreen() instanceof HasParent || //
        (transition != null && !transition.finished)) {
      canGoUp = true;
    }
    move(new Transition() {
      @Override public void execute() {
        Object current = backstack.current().getScreen();
        if (current instanceof HasParent<?>) {
          Object parent = ((HasParent) current).getParent();
          Backstack newBackstack =
              preserveEquivalentPrefix(backstack, Backstack.fromUpChain(parent));
          go(newBackstack, Direction.BACKWARD);
        } else {
          // We are not calling the listener, so we must complete this noop transition ourselves.
          onComplete();
        }
      }
    });
    return canGoUp;
  }

  /**
   * Go back one screen.
   *
   * @return false if going back is not possible.
   */
  public boolean goBack() {
    boolean canGoBack = backstack.size() > 1 || (transition != null && !transition.finished);
    move(new Transition() {
      @Override public void execute() {
        if (backstack.size() == 1) {
          // We are not calling the listener, so we must complete this noop transition ourselves.
          onComplete();
        } else {
          Backstack.Builder builder = backstack.buildUpon();
          builder.pop();
          Backstack newBackstack = builder.build();
          go(newBackstack, Direction.BACKWARD);
        }
      }
    });

    return canGoBack;
  }

  private void move(Transition transition) {
    if (this.transition == null || this.transition.finished) {
      this.transition = transition;
      transition.execute();
    } else {
      this.transition.enqueue(transition);
    }
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

  private abstract class Transition implements Callback {
    boolean finished;
    Transition next;
    Backstack nextBackstack;

    void enqueue(Transition transition) {
      if (this.next == null) {
        this.next = transition;
      } else {
        this.next.enqueue(transition);
      }
    }

    @Override public void onComplete() {
      if (finished) {
        throw new IllegalStateException("onComplete already called for this transition");
      }
      if (nextBackstack != null) {
        backstack = nextBackstack;
      }
      finished = true;
      if (next != null) {
        transition = next;
        transition.execute();
      }
    }

    protected void go(Backstack nextBackstack, Direction direction) {
      this.nextBackstack = nextBackstack;
      listener.go(nextBackstack, direction, this);
    }

    protected abstract void execute();
  }
}

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

import android.content.Context;
import java.util.Iterator;

/** Holds the current truth, the history of screens, and exposes operations to change it. */
public final class Flow {
  private static final String FLOW_SERVICE = "flow.Flow.FLOW_SERVICE";

  public static Flow get(Context context) {
    return (Flow) context.getSystemService(FLOW_SERVICE);
  }

  public static void loadInitialScreen(Context context) {
    Flow flow = get(context);
    Object screen = get(context).getBackstack().current().getPath();
    flow.resetTo(screen);
  }

  public static boolean isFlowSystemService(String name) {
    return FLOW_SERVICE.equals(name);
  }

  public enum Direction {
    FORWARD, BACKWARD, REPLACE
  }

  /** Supplied by Flow to the Listener, which is responsible for calling onComplete(). */
  public interface TraversalCallback {
    /**
     * Must be called exactly once to indicate that the corresponding transition has completed.
     *
     * If not called, the backstack will not be updated and further calls to Flow will not execute.
     * Calling more than once will result in an exception.
     */
    void onTraversalCompleted();
  }

  public static final class Traversal {
    /** May be null if this is a traversal into the start state. */
    public final Backstack origin;
    public final Backstack destination;
    public final Direction direction;

    private Traversal(Backstack from, Backstack to, Direction direction) {
      this.origin = from;
      this.destination = to;
      this.direction = direction;
    }
  }

  public interface Dispatcher {
    /**
     * Called when the backstack is about to change.  Note that Flow does not consider the Traversal
     * to be finished, and will not actually update the backstack, until the callback is triggered.
     *
     * @param callback Must be called to indicate completion of the traversal.
     */
    void dispatch(Traversal traversal, TraversalCallback callback);
  }

  private final Dispatcher dispatcher;
  private Backstack backstack;
  private PendingTraversal pendingTraversal;

  public Flow(Backstack backstack, Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
    this.backstack = backstack;
  }

  public Backstack getBackstack() {
    return backstack;
  }

  /** Push the screen onto the backstack. */
  public void goTo(final Object screen) {
    move(new PendingTraversal() {
      @Override public void execute() {
        Backstack newBackstack = backstack.buildUpon().push(screen).build();
        go(newBackstack, Direction.FORWARD);
      }
    });
  }

  /**
   * Reset to the specified screen. Pops until the screen is found.  If the screen is not found, the
   * entire backstack is replaced with the screen.
   */
  public void resetTo(final Object screen) {
    move(new PendingTraversal() {
      @Override public void execute() {
        Backstack.Builder builder = backstack.buildUpon();
        int count = 0;
        // Take care to leave the original screen instance on the stack, if we find it.  This enables
        // some arguably bad behavior on the part of clients, but it's still probably the right thing
        // to do.
        Object lastPopped = null;
        for (Iterator<Backstack.Entry> it = backstack.reverseIterator(); it.hasNext();) {
          Backstack.Entry entry = it.next();

          if (entry.getPath().equals(screen)) {
            // Clear up to the target screen.
            for (int i = 0; i < backstack.size() - count; i++) {
              lastPopped = builder.pop().getPath();
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
  public void replaceTo(final Object screen) {
    move(new PendingTraversal() {
      @Override public void execute() {
        Backstack newBackstack = preserveEquivalentPrefix(backstack, Backstack.fromUpChain(screen));
        go(newBackstack, Direction.REPLACE);
      }
    });
  }

  /**
   * Go up one screen.
   * @return false if going up is not possible.
   */
  public boolean goUp() {
    boolean canGoUp = false;
    if (backstack.current().getPath() instanceof HasParent || (pendingTraversal
        != null && !pendingTraversal.finished)) {
      canGoUp = true;
    }
    move(new PendingTraversal() {
      @Override public void execute() {
        Object current = backstack.current().getPath();
        if (current instanceof HasParent<?>) {
          Object parent = ((HasParent) current).getParent();
          Backstack newBackstack = preserveEquivalentPrefix(backstack, Backstack.fromUpChain(parent));
          go(newBackstack, Direction.BACKWARD);
        } else {
          // We are not calling the listener, so we must complete this noop transition ourselves.
          onTraversalCompleted();
        }
      }
    });
    return canGoUp;
  }

  /**
   * Go back one screen.
   * @return false if going back is not possible.
   */
  public boolean goBack() {
    boolean canGoBack = backstack.size() > 1 || (pendingTraversal != null && !pendingTraversal.finished);
    move(new PendingTraversal() {
      @Override public void execute() {
        if (backstack.size() == 1) {
          // We are not calling the listener, so we must complete this noop transition ourselves.
          onTraversalCompleted();
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

  /** Goes forward to a new backstack. */
  public void forward(final Backstack newBackstack) {
    move(new PendingTraversal() {
      @Override public void execute() {
        go(newBackstack, Direction.FORWARD);
      }
    });
  }

  /** Goes backward to a new backstack. */
  public void backward(final Backstack newBackstack) {
    move(new PendingTraversal() {
      @Override public void execute() {
        go(newBackstack, Direction.BACKWARD);
      }
    });
  }

  private void move(PendingTraversal pendingTraversal) {
    if (this.pendingTraversal == null || this.pendingTraversal.finished) {
      this.pendingTraversal = pendingTraversal;
      pendingTraversal.execute();
    } else {
      this.pendingTraversal.enqueue(pendingTraversal);
    }
  }

  private static Backstack preserveEquivalentPrefix(Backstack current, Backstack proposed) {
    Iterator<Backstack.Entry> oldIt = current.reverseIterator();
    Iterator<Backstack.Entry> newIt = proposed.reverseIterator();

    Backstack.Builder preserving =  Backstack.emptyBuilder();

    while (newIt.hasNext()) {
      Backstack.Entry newEntry = newIt.next();
      if (!oldIt.hasNext()) {
        preserving.push(newEntry.getPath());
        break;
      }
      Backstack.Entry oldEntry = oldIt.next();
      if (oldEntry.getPath().equals(newEntry.getPath())) {
        preserving.push(oldEntry.getPath());
      } else {
        preserving.push(newEntry.getPath());
        break;
      }
    }

    while (newIt.hasNext()) {
      preserving.push(newIt.next().getPath());
    }
    return preserving.build();
  }

  private abstract class PendingTraversal implements TraversalCallback {
    boolean finished;
    PendingTraversal next;
    Backstack nextBackstack;

    void enqueue(PendingTraversal pendingTraversal) {
      if (this.next == null) {
        this.next = pendingTraversal;
      } else {
        this.next.enqueue(pendingTraversal);
      }
    }

    @Override public void onTraversalCompleted() {
      if (finished) {
        throw new IllegalStateException("onComplete already called for this transition");
      }
      if (nextBackstack != null) {
        backstack = nextBackstack;
      }
      finished = true;
      if (next != null) {
        pendingTraversal = next;
        pendingTraversal.execute();
      }
    }

    protected void go(Backstack nextBackstack, Direction direction) {
      this.nextBackstack = nextBackstack;
      dispatcher.dispatch(new Traversal(getBackstack(), nextBackstack, direction), this);
    }

    protected abstract void execute();
  }
}

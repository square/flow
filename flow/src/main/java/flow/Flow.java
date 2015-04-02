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
import android.view.View;
import java.util.Iterator;

import static flow.Preconditions.checkNotNull;
import static java.lang.String.format;

/** Holds the current truth, the history of screens, and exposes operations to change it. */
public final class Flow {
  private static final String FLOW_SERVICE = "flow.Flow.FLOW_SERVICE";

  public static Flow get(View view) {
    return get(view.getContext());
  }

  public static Flow get(Context context) {
    //noinspection ResourceType
    return (Flow) context.getSystemService(FLOW_SERVICE);
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
     * Called when the backstack is about to change.  Note that Flow does not consider the
     * Traversal to be finished, and will not actually update the backstack, until the callback is
     * triggered. Traversals cannot be canceled.
     *
     * @param callback Must be called to indicate completion of the traversal.
     */
    void dispatch(Traversal traversal, TraversalCallback callback);
  }

  private Backstack backstack;
  private Dispatcher dispatcher;
  private PendingTraversal pendingTraversal;

  public Flow(Backstack backstack) {
    this.backstack = backstack;
  }

  public Backstack getBackstack() {
    return backstack;
  }

  /**
   * Set the dispatcher, may receive an immediate call to {@link Dispatcher#dispatch}. If a {@link
   * Traversal Traversal} is currently in progress with a previous Dispatcher, that Traversal will
   * not be affected.
   */
  public void setDispatcher(Dispatcher dispatcher) {
    this.dispatcher = checkNotNull(dispatcher, "dispatcher");

    if (pendingTraversal == null || //
        (pendingTraversal.state == TraversalState.DISPATCHED && pendingTraversal.next == null)) {
      // Nothing is happening;
      // OR, there is an outstanding callback and nothing will happen after it;
      // So enqueue a bootstrap traversal.
      setBackstack(backstack, Direction.REPLACE);
      return;
    }

    if (pendingTraversal.state == TraversalState.ENQUEUED) {
      // A traversal was enqueued while we had no dispatcher, run it now.
      pendingTraversal.execute();
      return;
    }

    if (pendingTraversal.state != TraversalState.DISPATCHED) {
      throw new AssertionError(
          format("Hanging traversal in unexpected state " + pendingTraversal.state));
    }
  }

  /**
   * Remove the dispatcher. A noop if the given dispatcher is not the current one.
   * <p>
   * No further {@link Traversal Traversals}, including Traversals currently enqueued, will execute
   * until a new dispatcher is set.
   */
  public void removeDispatcher(Dispatcher dispatcher) {
    // This mechanism protects against out of order calls to this method and setDispatcher
    // (e.g. if an outgoing activity is paused after an incoming one resumes).
    if (this.dispatcher == checkNotNull(dispatcher, "dispatcher")) this.dispatcher = null;
  }

  /**
   * Replaces the backstack with the one given and dispatches in the given direction.
   */
  public void setBackstack(final Backstack backstack, final Direction direction) {
    move(new PendingTraversal() {
      @Override void doExecute() {
        dispatch(backstack, direction);
      }
    });
  }

  /**
   * Updates the backstack such that the given path is at the top and dispatches the updated
   * backstack.
   *
   * If newTop is already at the top of the backstack, the backstack will be unchanged, but it will
   * be dispatched with direction {@link Direction#REPLACE}.
   *
   * If newTop is already on the backstack but not at the top, the stack will pop until newTop is
   * at the top, and the dispatch direction will be {@link Direction#BACKWARD}.
   *
   * If newTop is not already on the backstack, it will be pushed and the dispatch direction will be
   * {@link Direction#FORWARD}.
   *
   * Paths equality is always checked using {@link Path#equals(Object)}.
   */
  public void set(final Path newTop) {
    move(new PendingTraversal() {
      @Override void doExecute() {
        if (newTop.equals(backstack.current())) {
          dispatch(backstack, Direction.REPLACE);
          return;
        }

        Backstack.Builder builder = backstack.buildUpon();
        int count = 0;
        // Search backward to see if we already have newTop on the stack
        Path preservedInstance = null;
        for (Iterator<Path> it = backstack.reverseIterator(); it.hasNext(); ) {
          Path entry = it.next();

          // If we find newTop on the stack, pop back to it.
          if (entry.equals(newTop)) {
            for (int i = 0; i < backstack.size() - count; i++) {
              preservedInstance = builder.pop();
            }
            break;
          } else {
            count++;
          }
        }

        Backstack newBackstack;
        if (preservedInstance != null) {
          // newTop was on the backstack. Put the preserved instance back on and dispatch.
          builder.push(preservedInstance);
          newBackstack = builder.build();
          dispatch(newBackstack, Direction.BACKWARD);
        } else {
          // newTop was not on the backstack. Push it on and dispatch.
          builder.push(newTop);
          newBackstack = builder.build();
          dispatch(newBackstack, Direction.FORWARD);
        }
      }
    });
  }

  /**
   * Push the screen onto the backstack.
   *
   * @deprecated Use {@link #set(Path)}.
   */
  @Deprecated @SuppressWarnings("UnusedDeclaration") public void goTo(final Path path) {
    set(path);
  }

  /**
   * @deprecated Use {@link #set(Path)}.
   */
  @Deprecated @SuppressWarnings("UnusedDeclaration") public void resetTo(final Path path) {
    set(path);
  }

  /**
   * @deprecated Use {@link #setBackstack(Backstack, Direction)}.
   */
  @Deprecated  @SuppressWarnings("deprecation") public void replaceTo(final Path path) {
    move(new PendingTraversal() {
     @Override protected void doExecute() {
        Backstack newBackstack = preserveEquivalentPrefix(backstack, Backstack.fromUpChain(path));
        dispatch(newBackstack, Direction.REPLACE);
      }
    });
  }

  /**
   * Go up one screen.
   *
   * @return false if going up is not possible.
   * @deprecated Use {@link #setBackstack(Backstack, Direction)}
   */
  @Deprecated @SuppressWarnings("deprecation") public boolean goUp() {
    boolean canGoUp = false;
    if (backstack.current() instanceof HasParent || (pendingTraversal != null
        && pendingTraversal.state != TraversalState.FINISHED)) {
      canGoUp = true;
    }
    move(new PendingTraversal() {
       @Override public void doExecute() {
        Path current = backstack.current();
        if (current instanceof HasParent) {
          Path parent = ((HasParent) current).getParent();
          Backstack newBackstack =
              preserveEquivalentPrefix(backstack, Backstack.fromUpChain(parent));
          dispatch(newBackstack, Direction.BACKWARD);
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
   *
   * @return false if going back is not possible.
   */
  public boolean goBack() {
    boolean canGoBack = backstack.size() > 1 || (pendingTraversal != null
        && pendingTraversal.state != TraversalState.FINISHED);
    move(new PendingTraversal() {
      @Override protected void doExecute() {
        if (backstack.size() == 1) {
          // We are not calling the listener, so we must complete this noop transition ourselves.
          onTraversalCompleted();
        } else {
          Backstack.Builder builder = backstack.buildUpon();
          builder.pop();
          Backstack newBackstack = builder.build();
          dispatch(newBackstack, Direction.BACKWARD);
        }
      }
    });

    return canGoBack;
  }

  /**
   * Goes forward to a new backstack.
   *
   * @deprecated Use {@link #setBackstack(Backstack, Direction)}
   */
  @Deprecated @SuppressWarnings("UnusedDeclaration")
  public void forward(final Backstack newBackstack) {
    setBackstack(newBackstack, Direction.FORWARD);
  }

  /**
   * Goes backward to a new backstack.
   *
   * @deprecated Use {@link #setBackstack(Backstack, Direction)}
   */
  @Deprecated @SuppressWarnings("UnusedDeclaration")
  public void backward(final Backstack newBackstack) {
    setBackstack(newBackstack, Direction.BACKWARD);
  }

  private void move(PendingTraversal pendingTraversal) {
    if (this.pendingTraversal == null) {
      this.pendingTraversal = pendingTraversal;
      // If there is no dispatcher wait until one shows up before executing.
      if (dispatcher != null) pendingTraversal.execute();
    } else {
      this.pendingTraversal.enqueue(pendingTraversal);
    }
  }

  private static Backstack preserveEquivalentPrefix(Backstack current, Backstack proposed) {
    Iterator<Path> oldIt = current.reverseIterator();
    Iterator<Path> newIt = proposed.reverseIterator();

    Backstack.Builder preserving = Backstack.emptyBuilder();

    while (newIt.hasNext()) {
      Path newEntry = newIt.next();
      if (!oldIt.hasNext()) {
        preserving.push(newEntry);
        break;
      }
      Path oldEntry = oldIt.next();
      if (oldEntry.equals(newEntry)) {
        preserving.push(oldEntry);
      } else {
        preserving.push(newEntry);
        break;
      }
    }

    while (newIt.hasNext()) {
      preserving.push(newIt.next());
    }
    return preserving.build();
  }

  private enum TraversalState {
    /** {@link PendingTraversal#execute} has not been called. */
    ENQUEUED,
    /**
     * {@link PendingTraversal#execute} was called, waiting for {@link
     * PendingTraversal#onTraversalCompleted}.
     */
    DISPATCHED,
    /**
     * {@link PendingTraversal#onTraversalCompleted} was called.
     */
    FINISHED
  }

  private abstract class PendingTraversal implements TraversalCallback {

    TraversalState state = TraversalState.ENQUEUED;
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
      if (state != TraversalState.DISPATCHED) {
        throw new IllegalStateException(
            state == TraversalState.FINISHED ? "onComplete already called for this transition"
                : "transition not yet dispatched!");
      }
      // Is not set by noop transitions.
      if (nextBackstack != null) {
        backstack = nextBackstack;
      }
      state = TraversalState.FINISHED;
      pendingTraversal = next;
      if (dispatcher != null && pendingTraversal != null) {
        pendingTraversal.execute();
      }
    }

    void dispatch(Backstack nextBackstack, Direction direction) {
      this.nextBackstack = checkNotNull(nextBackstack, "nextBackstack");
      if (dispatcher == null) {
        throw new AssertionError("Bad doExecute method allowed dispatcher to be cleared");
      }
      dispatcher.dispatch(new Traversal(getBackstack(), nextBackstack, direction), this);
    }

    final void execute() {
      if (state != TraversalState.ENQUEUED) throw new AssertionError("unexpected state " + state);
      if (dispatcher == null) throw new AssertionError("Caller must ensure that dispatcher is set");

      state = TraversalState.DISPATCHED;
      doExecute();
    }

    /**
     * Must be synchronous and end with a call to {@link #dispatch} or {@link #onTraversalCompleted()}.
     */
    abstract void doExecute();
  }
}

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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import java.util.Iterator;

import static flow.Preconditions.checkArgument;
import static flow.Preconditions.checkNotNull;

/** Holds the current truth, the history of screens, and exposes operations to change it. */
public final class Flow {
  static final String HISTORY_KEY = InternalLifecycleIntegration.class.getSimpleName() + "_history";

  public static Flow get(View view) {
    return get(view.getContext());
  }

  public static Flow get(Context context) {
    return InternalContextWrapper.getFlow(context);
  }

  public static Installer configure(Context baseContext, Activity activity) {
    return new Installer(baseContext, activity);
  }

  public static boolean onBackPressed(Activity activity) {
    return get(activity).goBack();
  }

  public static void setHistoryExtra(Intent intent, History history, StateParceler parceler) {
    intent.putExtra(HISTORY_KEY, history.getParcelable(parceler));
  }

  public static void onNewIntent(Intent intent, Activity activity) {
    checkArgument(intent != null, "intent may not be null");
    if (intent.hasExtra(HISTORY_KEY)) {
      InternalLifecycleIntegration.find(activity).onNewIntent(intent);
    }
  }

  public enum Direction {
    FORWARD, BACKWARD, REPLACE
  }

  /** Supplied by Flow to the Listener, which is responsible for calling onComplete(). */
  public interface TraversalCallback {
    /**
     * Must be called exactly once to indicate that the corresponding transition has completed.
     *
     * If not called, the history will not be updated and further calls to Flow will not execute.
     * Calling more than once will result in an exception.
     */
    void onTraversalCompleted();
  }

  public static final class Traversal {
    /** May be null if this is a traversal into the start state. */
    public final History origin;
    public final History destination;
    public final Direction direction;

    private Traversal(History from, History to, Direction direction) {
      this.origin = from;
      this.destination = to;
      this.direction = direction;
    }
  }

  public interface Dispatcher {
    /**
     * Called when the history is about to change.  Note that Flow does not consider the
     * Traversal to be finished, and will not actually update the history, until the callback is
     * triggered. Traversals cannot be canceled.
     *
     * @param callback Must be called to indicate completion of the traversal.
     */
    void dispatch(Traversal traversal, TraversalCallback callback);
  }

  private History history;
  private Dispatcher dispatcher;
  private PendingTraversal pendingTraversal;

  public Flow(History history) {
    this.history = history;
  }

  public History getHistory() {
    return history;
  }

  /**
   * Set the dispatcher, may receive an immediate call to {@link Dispatcher#dispatch}. If a {@link
   * Traversal Traversal} is currently in progress with a previous Dispatcher, that Traversal will
   * not be affected.
   */
  void setDispatcher(Dispatcher dispatcher) {
    this.dispatcher = checkNotNull(dispatcher, "dispatcher");

    if (pendingTraversal == null || //
        (pendingTraversal.state == TraversalState.DISPATCHED && pendingTraversal.next == null)) {
      // Nothing is happening;
      // OR, there is an outstanding callback and nothing will happen after it;
      // So enqueue a bootstrap traversal.
      setHistory(history, Direction.REPLACE);
      return;
    }

    if (pendingTraversal.state == TraversalState.ENQUEUED) {
      // A traversal was enqueued while we had no dispatcher, run it now.
      pendingTraversal.execute();
      return;
    }

    if (pendingTraversal.state != TraversalState.DISPATCHED) {
      throw new AssertionError("Hanging traversal in unexpected state " + pendingTraversal.state);
    }
  }

  /**
   * Remove the dispatcher. A noop if the given dispatcher is not the current one.
   * <p>
   * No further {@link Traversal Traversals}, including Traversals currently enqueued, will execute
   * until a new dispatcher is set.
   */
  void removeDispatcher(Dispatcher dispatcher) {
    // This mechanism protects against out of order calls to this method and setDispatcher
    // (e.g. if an outgoing activity is paused after an incoming one resumes).
    if (this.dispatcher == checkNotNull(dispatcher, "dispatcher")) this.dispatcher = null;
  }

  /**
   * Replaces the history with the one given and dispatches in the given direction.
   */
  public void setHistory(final History history, final Direction direction) {
    move(new PendingTraversal() {
      @Override void doExecute() {
        dispatch(preserveEquivalentPrefix(getHistory(), history), direction);
      }
    });
  }

  /**
   * Updates the history such that the given object is at the top and dispatches the updated
   * history.
   *
   * If newTop is already at the top of the history, the history will be unchanged, but it will
   * be dispatched with direction {@link Direction#REPLACE}.
   *
   * If newTop is already on the history but not at the top, the stack will pop until newTop is
   * at the top, and the dispatch direction will be {@link Direction#BACKWARD}.
   *
   * If newTop is not already on the history, it will be pushed and the dispatch direction will be
   * {@link Direction#FORWARD}.
   *
   * Objects equality is always checked using {@link Object#equals(Object)}.
   */
  public void set(final Object newTop) {
    move(new PendingTraversal() {
      @Override void doExecute() {
        if (newTop.equals(history.top())) {
          dispatch(history, Direction.REPLACE);
          return;
        }

        History.Builder builder = history.buildUpon();
        int count = 0;
        // Search backward to see if we already have newTop on the stack
        Object preservedInstance = null;
        for (Iterator<Object> it = history.reverseIterator(); it.hasNext(); ) {
          Object entry = it.next();

          // If we find newTop on the stack, pop back to it.
          if (entry.equals(newTop)) {
            for (int i = 0; i < history.size() - count; i++) {
              preservedInstance = builder.pop();
            }
            break;
          } else {
            count++;
          }
        }

        History newHistory;
        if (preservedInstance != null) {
          // newTop was on the history. Put the preserved instance back on and dispatch.
          builder.push(preservedInstance);
          newHistory = builder.build();
          dispatch(newHistory, Direction.BACKWARD);
        } else {
          // newTop was not on the history. Push it on and dispatch.
          builder.push(newTop);
          newHistory = builder.build();
          dispatch(newHistory, Direction.FORWARD);
        }
      }
    });
  }

  /**
   * Go back one screen.
   *
   * @return false if going back is not possible.
   */
  public boolean goBack() {
    boolean canGoBack = history.size() > 1 || (pendingTraversal != null
        && pendingTraversal.state != TraversalState.FINISHED);
    move(new PendingTraversal() {
      @Override protected void doExecute() {
        if (history.size() == 1) {
          // We are not calling the listener, so we must complete this noop transition ourselves.
          onTraversalCompleted();
        } else {
          History.Builder builder = history.buildUpon();
          builder.pop();
          History newHistory = builder.build();
          dispatch(newHistory, Direction.BACKWARD);
        }
      }
    });

    return canGoBack;
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

  private static History preserveEquivalentPrefix(History current, History proposed) {
    Iterator<Object> oldIt = current.reverseIterator();
    Iterator<Object> newIt = proposed.reverseIterator();

    History.Builder preserving = current.buildUpon().clear();

    while (newIt.hasNext()) {
      Object newEntry = newIt.next();
      if (!oldIt.hasNext()) {
        preserving.push(newEntry);
        break;
      }
      Object oldEntry = oldIt.next();
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
    History nextHistory;

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
      if (nextHistory != null) {
        history = nextHistory;
      }
      state = TraversalState.FINISHED;
      pendingTraversal = next;
      if (dispatcher != null && pendingTraversal != null) {
        pendingTraversal.execute();
      }
    }

    void dispatch(History nextHistory, Direction direction) {
      this.nextHistory = checkNotNull(nextHistory, "nextHistory");
      if (dispatcher == null) {
        throw new AssertionError("Bad doExecute method allowed dispatcher to be cleared");
      }
      dispatcher.dispatch(new Traversal(getHistory(), nextHistory, direction), this);
    }

    final void execute() {
      if (state != TraversalState.ENQUEUED) throw new AssertionError("unexpected state " + state);
      if (dispatcher == null) throw new AssertionError("Caller must ensure that dispatcher is set");

      state = TraversalState.DISPATCHED;
      doExecute();
    }

    /**
     * Must be synchronous and end with a call to {@link #dispatch} or {@link
     * #onTraversalCompleted()}.
     */
    abstract void doExecute();
  }
}

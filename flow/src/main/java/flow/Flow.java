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
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static flow.Preconditions.checkArgument;
import static flow.Preconditions.checkNotNull;

/** Holds the current truth, the history of screens, and exposes operations to change it. */
public final class Flow {
  static final Object ROOT_KEY = new Object() {
    @Override public String toString() {
      return Flow.class.getName() + ".ROOT_KEY";
    }
  };

  @NonNull public static Flow get(@NonNull View view) {
    return get(view.getContext());
  }

  @NonNull public static Flow get(@NonNull Context context) {
    return InternalContextWrapper.getFlow(context);
  }

  /** @return null if context has no Flow key embedded. */
  @Nullable public static <T> T getKey(@NonNull Context context) {
    final FlowContextWrapper wrapper = FlowContextWrapper.get(context);
    if (wrapper == null) return null;
    return wrapper.services.getKey();
  }

  /** @return null if view's Context has no Flow key embedded. */
  @Nullable public static <T> T getKey(@NonNull View view) {
    return getKey(view.getContext());
  }

  /** @return null if context does not contain the named service. */
  @Nullable public static <T> T getService(@NonNull String serviceName, @NonNull Context context) {
    final FlowContextWrapper wrapper = FlowContextWrapper.get(context);
    if (wrapper == null) return null;
    return wrapper.services.getService(serviceName);
  }

  @NonNull
  public static Installer configure(@NonNull Context baseContext, @NonNull Activity activity) {
    return new Installer(baseContext, activity);
  }

  /** Adds a history as an extra to an Intent. */
  public static void addHistory(@NonNull Intent intent, @NonNull History history,
      @NonNull KeyParceler parceler) {
    InternalLifecycleIntegration.addHistoryToIntent(intent, history, parceler);
  }

  /**
   * Handles an Intent carrying a History extra.
   *
   * @return true if the Intent contains a History and it was handled.
   */
  @CheckResult public static boolean onNewIntent(@NonNull Intent intent,
      @NonNull Activity activity) {
    //noinspection ConstantConditions
    checkArgument(intent != null, "intent may not be null");
    if (intent.hasExtra(InternalLifecycleIntegration.INTENT_KEY)) {
      InternalLifecycleIntegration.find(activity).onNewIntent(intent);
      return true;
    }
    return false;
  }

  private History history;
  private Dispatcher dispatcher;
  private PendingTraversal pendingTraversal;
  private List<Object> tearDownKeys = new ArrayList<>();
  private final KeyManager keyManager;

  Flow(KeyManager keyManager, History history) {
    this.keyManager = keyManager;
    this.history = history;
  }

  @NonNull public History getHistory() {
    return history;
  }

  /**
   * Set the dispatcher, may receive an immediate call to {@link Dispatcher#dispatch}. If a {@link
   * Traversal Traversal} is currently in progress with a previous Dispatcher, that Traversal will
   * not be affected.
   */
  public void setDispatcher(@NonNull Dispatcher dispatcher) {
    setDispatcher(dispatcher, false);
  }

  void setDispatcher(@NonNull Dispatcher dispatcher, final boolean restore) {
    this.dispatcher = checkNotNull(dispatcher, "dispatcher");

    if (pendingTraversal == null || //
        (pendingTraversal.state == TraversalState.DISPATCHED && pendingTraversal.next == null)) {
      // Nothing is happening;
      // OR, there is an outstanding callback and nothing will happen after it;
      // So enqueue a bootstrap traversal.
      move(new PendingTraversal() {
        @Override void doExecute() {
          bootstrap(history, restore);
        }
      });
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
  public void removeDispatcher(@NonNull Dispatcher dispatcher) {
    // This mechanism protects against out of order calls to this method and setDispatcher
    // (e.g. if an outgoing activity is paused after an incoming one resumes).
    if (this.dispatcher == checkNotNull(dispatcher, "dispatcher")) this.dispatcher = null;
  }

  /**
   * Replaces the history with the one given and dispatches in the given direction.
   */
  public void setHistory(@NonNull final History history, @NonNull final Direction direction) {
    move(new PendingTraversal() {
      @Override void doExecute() {
        dispatch(preserveEquivalentPrefix(getHistory(), history), direction);
      }
    });
  }

  /**
   * Replaces the history with the given key and dispatches in the given direction.
   */
  public void replaceHistory(@NonNull final Object key, @NonNull final Direction direction) {
    setHistory(getHistory().buildUpon().clear().push(key).build(), direction);
  }

  /**
   * Replaces the top key of the history with the given key and dispatches in the given direction.
   */
  public void replaceTop(@NonNull final Object key, @NonNull final Direction direction) {
    setHistory(getHistory().buildUpon().pop(1).push(key).build(), direction);
  }

  /**
   * Updates the history such that the given key is at the top and dispatches the updated
   * history.
   *
   * If newTopKey is already at the top of the history, the history will be unchanged, but it will
   * be dispatched with direction {@link Direction#REPLACE}.
   *
   * If newTopKey is already on the history but not at the top, the stack will pop until newTopKey
   * is at the top, and the dispatch direction will be {@link Direction#BACKWARD}.
   *
   * If newTopKey is not already on the history, it will be pushed and the dispatch direction will
   * be {@link Direction#FORWARD}.
   *
   * Objects' equality is always checked using {@link Object#equals(Object)}.
   */
  public void set(@NonNull final Object newTopKey) {
    move(new PendingTraversal() {
      @Override void doExecute() {
        if (newTopKey.equals(history.top())) {
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
          if (entry.equals(newTopKey)) {
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
          builder.push(newTopKey);
          newHistory = builder.build();
          dispatch(newHistory, Direction.FORWARD);
        }
      }
    });
  }

  /**
   * Go back one key.
   *
   * @return false if going back is not possible or a traversal is in progress.
   */
  @CheckResult public boolean goBack() {
    boolean canGoBack = history.size() > 1 || (pendingTraversal != null
        && pendingTraversal.state != TraversalState.FINISHED);
    if (!canGoBack) return false;
    History.Builder builder = history.buildUpon();
    builder.pop();
    final History newHistory = builder.build();
    setHistory(newHistory, Direction.BACKWARD);
    return true;
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
      // Is not set by noop and bootstrap transitions.
      if (nextHistory != null) {
        tearDownKeys.add(history.top());
        history = nextHistory;
      }
      state = TraversalState.FINISHED;
      pendingTraversal = next;

      if (pendingTraversal == null) {
        final Iterator<Object> it = tearDownKeys.iterator();
        while (it.hasNext()) {
          keyManager.tearDown(it.next());
          it.remove();
        }
        keyManager.clearStatesExcept(history.asList());
      } else if (dispatcher != null) {
        pendingTraversal.execute();
      }
    }

    void bootstrap(History history, boolean restore) {
      if (dispatcher == null) {
        throw new AssertionError("Bad doExecute method allowed dispatcher to be cleared");
      }
      if (!restore) {
        keyManager.setUp(history.top());
      }
      dispatcher.dispatch(new Traversal(null, history, Direction.REPLACE, keyManager), this);
    }

    void dispatch(History nextHistory, Direction direction) {
      this.nextHistory = checkNotNull(nextHistory, "nextHistory");
      if (dispatcher == null) {
        throw new AssertionError("Bad doExecute method allowed dispatcher to be cleared");
      }
      keyManager.setUp(nextHistory.top());
      dispatcher.dispatch(new Traversal(getHistory(), nextHistory, direction, keyManager), this);
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

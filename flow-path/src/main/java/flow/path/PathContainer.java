/*
 * Copyright 2014 Square Inc.
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

package flow.path;

import android.view.View;
import android.view.ViewGroup;
import flow.Flow;
import flow.State;

import static flow.path.Preconditions.checkNotNull;

/**
 * Handles swapping paths within a container view, as well as flow mechanics, allowing supported
 * container views to be largely declarative.
 */
public abstract class PathContainer {

  /**
   * Provides information about the current or most recent Traversal handled by the container.
   */
  protected static final class TraversalState {
    private Path fromPath;
    private State fromState;
    private Path toPath;
    private State toState;

    public void setNextEntry(Path path, State State) {
      this.fromPath = this.toPath;
      this.fromState = this.toState;
      this.toPath = path;
      this.toState = State;
    }

    public Path fromPath() {
      return fromPath;
    }

    public Path toPath() {
      return toPath;
    }

    public void saveViewState(View view) {
      fromState.save(view);
    }

    public void restoreViewState(View view) {
      toState.restore(view);
    }
  }

  private final int tagKey;

  /**
   * @param tagKey an id used to store bookkeeping info on container views via {@link
   * View#setTag(int, Object)}
   */
  protected PathContainer(int tagKey) {
    this.tagKey = tagKey;
  }

  public final void executeTraversal(PathContainerView view, Flow.Traversal traversal,
      final Flow.TraversalCallback callback) {
    final View oldChild = view.getCurrentChild();
    ViewGroup containerView = view.getContainerView();
    State State = traversal.destination.topSaveState();
    doShowPath(traversal.destination.<Path>top(), containerView, oldChild, traversal.direction,
        State, callback);
  }

  /**
   * Replaces the contents of a given {@link ViewGroup} with a new view inflated from
   * a {@link Flow.Traversal}.
   */
  public final void executeFlowTraversal(ViewGroup container, Flow.Traversal traversal,
      final Flow.TraversalCallback callback) {
    final View oldChild = container.getChildAt(0);
    State State = traversal.destination.topSaveState();
    doShowPath(traversal.destination.<Path>top(), container, oldChild, traversal.direction, State, callback);
  }

  /**
   * Replace the current view and show the given path. Allows display of {@link Path}s other
   * than in response to Flow dispatches.
   */
  public void setPath(ViewGroup container, Path path, Flow.Direction direction,
      Flow.TraversalCallback callback) {
    doShowPath(path, container, container.getChildAt(0), direction, State.empty(path), callback);
  }

  private void doShowPath(Path path, ViewGroup container, View oldChild, Flow.Direction direction,
      State State, Flow.TraversalCallback callback) {
    Path oldPath;
    TraversalState traversalState = ensureTag(container);

    // See if we already have the direct child we want, and if so short circuit the traversal.
    if (oldChild != null) {
      oldPath = checkNotNull(traversalState.toPath, "Container view has child %s with no path",
          oldChild.toString());
      if (oldPath.equals(path)) {
        callback.onTraversalCompleted();
        return;
      }
    }
    
    if(traversal.direction == Flow.Direction.FORWARD) {
        Iterator<Object> destinationPaths = traversal.destination.reverseIterator();
        int i = 1; // 0 is ROOT in PathContext, we need to merge destination paths after the ROOT without adding self-duplication
        while(_destinationPaths.hasNext()) {
            Path destinationPath = (Path) destinationPaths.next();
            if(destinationPath != path) {
                path.elements().add(i, destinationPath);
            }
            i++;
        }
    }

    traversalState.setNextEntry(path, State);
    performTraversal(container, traversalState, direction, callback);
  }

  protected abstract void performTraversal(ViewGroup container, TraversalState traversalState,
      Flow.Direction direction, Flow.TraversalCallback callback);

  private TraversalState ensureTag(ViewGroup container) {
    TraversalState traversalState = (TraversalState) container.getTag(tagKey);
    if (traversalState == null) {
      traversalState = new TraversalState();
      container.setTag(tagKey, traversalState);
    }
    return traversalState;
  }
}

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
import flow.Saved;

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
    private Saved fromSaved;
    private Path toPath;
    private Saved toSaved;

    public void setNextEntry(Path path, Saved saved) {
      this.fromPath = this.toPath;
      this.fromSaved = this.toSaved;
      this.toPath = path;
      this.toSaved = saved;
    }

    public Path fromPath() {
      return fromPath;
    }

    public Path toPath() {
      return toPath;
    }

    public void saveViewState(View view) {
      fromSaved.save(view);
    }

    public void restoreViewState(View view) {
      toSaved.restore(view);
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
    Saved saved = traversal.destination.topSaveState();
    doShowPath(traversal.destination.<Path>top(), containerView, oldChild, traversal.direction,
        saved, callback);
  }

  /**
   * Replaces the contents of a given {@link ViewGroup} with a new view inflated from
   * a {@link Flow.Traversal}.
   */
  public final void executeFlowTraversal(ViewGroup container, Flow.Traversal traversal,
      final Flow.TraversalCallback callback) {
    final View oldChild = container.getChildAt(0);
    Saved saved = traversal.destination.topSaveState();
    doShowPath(traversal.destination.<Path>top(), container, oldChild, traversal.direction, saved, callback);
  }

  /**
   * Replace the current view and show the given path. Allows display of {@link Path}s other
   * than in response to Flow dispatches.
   */
  public void setPath(ViewGroup container, Path path, Flow.Direction direction,
      Flow.TraversalCallback callback) {
    doShowPath(path, container, container.getChildAt(0), direction, Saved.NULL, callback);
  }

  private void doShowPath(Path path, ViewGroup container, View oldChild, Flow.Direction direction,
      Saved saved, Flow.TraversalCallback callback) {
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

    traversalState.setNextEntry(path, saved);
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

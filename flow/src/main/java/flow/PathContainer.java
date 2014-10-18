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

package flow;

import android.view.View;
import android.view.ViewGroup;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles swapping paths within a container view, as well as flow mechanics, allowing supported
 * container views to be largely declarative.
 */
public abstract class PathContainer {
  public abstract static class Factory {
    protected final int tagKey;
    protected final PathContextFactory contextFactory;

    public Factory(int tagKey, PathContextFactory contextFactory) {
      this.tagKey = tagKey;
      this.contextFactory = contextFactory;
    }

    public abstract PathContainer createPathContainer(PathContainerView view);
  }

  /**
   * Provides information about the current or most recent Traversal handled by the container.
   */
  protected static final class TraversalState {
    private Backstack.Entry fromEntry;
    private Backstack.Entry toEntry;

    public void setNextEntry(Backstack.Entry entry) {
      this.fromEntry = this.toEntry;
      this.toEntry = entry;
    }

    public Path fromPath() {
      return fromEntry == null ? null : fromEntry.getPath();
    }

    public Path toPath() {
      return toEntry.getPath();
    }

    public void saveViewState(View view) {
      fromEntry.saveViewState(view);
    }

    public void restoreViewState(View view) {
      toEntry.restoreViewState(view);
    }
  }

  private static final Map<Class, Integer> PATH_LAYOUT_CACHE = new LinkedHashMap<>();

  private final PathContainerView view;
  private final int tagKey;

  protected PathContainer(PathContainerView view, int tagKey) {
    this.view = view;
    this.tagKey = tagKey;
  }

  public final void executeTraversal(Flow.Traversal traversal,
      final Flow.TraversalCallback callback) {
    final View oldChild = view.getCurrentChild();
    Backstack.Entry entry = traversal.destination.currentEntry();
    Backstack.Entry oldEntry;
    ViewGroup containerView = view.getContainerView();
    TraversalState traversalState = ensureTag(containerView);

    // See if we already have the direct child we want, and if so short circuit the traversal.
    if (oldChild != null) {
      oldEntry = Preconditions.checkNotNull(traversalState.toEntry,
          "Container view has child %s with no path", oldChild.toString());
      if (oldEntry.equals(entry)) {
        callback.onTraversalCompleted();
        return;
      }
    }

    traversalState.setNextEntry(entry);

    performTraversal(containerView, traversalState, traversal.direction, callback);
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

  protected int getLayout(Path path) {
    Class<Object> pathType = ObjectUtils.getClass(path);
    Integer layoutResId = PATH_LAYOUT_CACHE.get(pathType);
    if (layoutResId == null) {
      Layout layout = pathType.getAnnotation(Layout.class);
      Preconditions.checkNotNull(layout, "@%s annotation not found on class %s",
          Layout.class.getSimpleName(), pathType.getName());
      layoutResId = layout.value();
      PATH_LAYOUT_CACHE.put(pathType, layoutResId);
    }
    return layoutResId;
  }
}

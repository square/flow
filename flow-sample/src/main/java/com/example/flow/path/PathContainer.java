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

package com.example.flow.path;

import android.view.View;
import android.view.ViewGroup;
import com.example.flow.util.ObjectUtils;
import flow.Flow;
import flow.Layout;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.flow.util.Preconditions.checkNotNull;

/**
 * Handles swapping paths within a container view, as well as flow mechanics, allowing supported
 * container views to be largely declarative.
 *
 * This takes care of saving the swapped out view state into its screen, so that the view state can
 * be restored when coming back in the flow. That backstack view state will also be preserved when
 * the activity is saved (e.g. on rotation or when pressing home).
 */
public abstract class PathContainer {
  public abstract static class Factory {
    protected final int tagKey;
    protected final PathContextFactory contextFactory;

    public Factory(int tagKey, PathContextFactory contextFactory) {
      this.tagKey = tagKey;
      this.contextFactory = contextFactory;
    }

    public abstract PathContainer createPathController(PathContainerView view);
  }

  /**
   * Set on the container view to make screen information available during a transition.
   * TODO(rjrjr) Does this really belong here or down in {@link com.example.flow.pathview.SimplePathContainer}?
   */
  protected static final class Tag {
    public Path fromPath;
    public Path toPath;

    public void setNextScreen(Path path) {
      this.fromPath = this.toPath;
      this.toPath = path;
    }
  }

  private static final Map<Class, Integer> SCREEN_LAYOUT_CACHE = new LinkedHashMap<>();

  private final PathContainerView view;
  protected final int tagKey;

  protected PathContainer(PathContainerView view, int tagKey) {
    this.view = view;
    this.tagKey = tagKey;
  }

  public void showPath(Path path, Flow.Direction direction, final Flow.TraversalCallback callback) {
    final View oldChild = view.getCurrentChild();
    Path oldChildPath = null;

    // See if we already have the direct child we want, and if so delegate the transition.
    if (oldChild != null) {
      Tag tag = (Tag) view.getContainerView().getTag(tagKey);
      oldChildPath = checkNotNull(tag.toPath, "Container view has child %s with no screen",
          oldChild.toString());
      if (oldChildPath.getName().equals(path.getName())) {
        callback.onTraversalCompleted();
        return;
      }
    }

    transition(view.getContainerView(), oldChildPath, path, direction, callback);
  }

  protected abstract void transition(ViewGroup container, Path from, Path to,
      Flow.Direction direction, Flow.TraversalCallback callback);

  protected Tag ensureTag(ViewGroup container) {
    Tag tag = (Tag) container.getTag(tagKey);
    if (tag == null) {
      tag = new Tag();
      container.setTag(tagKey, tag);
    }
    return tag;
  }

  protected static int getLayout(Object screen) {
    Class<Object> screenType = ObjectUtils.getClass(screen);
    Integer layoutResId = SCREEN_LAYOUT_CACHE.get(screenType);
    if (layoutResId == null) {
      Layout layout = screenType.getAnnotation(Layout.class);
      checkNotNull(layout, "@%s annotation not found on class %s", Layout.class.getSimpleName(),
          screenType.getName());
      layoutResId = layout.value();
      SCREEN_LAYOUT_CACHE.put(screenType, layoutResId);
    }
    return layoutResId;
  }
}

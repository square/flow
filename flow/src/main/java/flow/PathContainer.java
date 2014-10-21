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
   * TODO(rjrjr) Does this really belong here or down in SimplePathContainer?
   */
  protected static final class Tag {
    public Backstack.Entry fromEntry;
    public Backstack.Entry toEntry;

    public void setNextEntry(Backstack.Entry entry) {
      this.fromEntry = this.toEntry;
      this.toEntry = entry;
    }
  }

  private static final Map<Class, Integer> SCREEN_LAYOUT_CACHE = new LinkedHashMap<>();

  private final PathContainerView view;
  protected final int tagKey;

  protected PathContainer(PathContainerView view, int tagKey) {
    this.view = view;
    this.tagKey = tagKey;
  }

  public final void executeTraversal(Flow.Traversal traversal, final Flow.TraversalCallback callback) {
    final View oldChild = view.getCurrentChild();
    Backstack.Entry entry = traversal.destination.current();
    Backstack.Entry oldEntry = null;

    // See if we already have the direct child we want, and if so delegate the transition.
    if (oldChild != null) {
      Tag tag = (Tag) view.getContainerView().getTag(tagKey);
      oldEntry = Preconditions.checkNotNull(tag.toEntry,
          "Container view has child %s with no screen", oldChild.toString());
      if (oldEntry.equals(entry)) {
        callback.onTraversalCompleted();
        return;
      }
    }

    performTraversal(view.getContainerView(), oldEntry, entry, traversal.direction, callback);
  }

  protected abstract void performTraversal(ViewGroup container, Backstack.Entry from, Backstack.Entry to,
      Flow.Direction direction, Flow.TraversalCallback callback);

  protected final Tag ensureTag(ViewGroup container) {
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
      Preconditions.checkNotNull(layout, "@%s annotation not found on class %s",
          Layout.class.getSimpleName(), screenType.getName());
      layoutResId = layout.value();
      SCREEN_LAYOUT_CACHE.put(screenType, layoutResId);
    }
    return layoutResId;
  }
}

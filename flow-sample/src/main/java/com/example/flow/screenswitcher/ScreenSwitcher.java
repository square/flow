package com.example.flow.screenswitcher;

import android.os.Parcelable;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.flow.appflow.PathContext;
import com.example.flow.appflow.Screen;
import com.example.flow.appflow.ScreenContextFactory;
import com.example.flow.util.ObjectUtils;
import flow.Layout;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.flow.util.Preconditions.checkNotNull;

/**
 * Handles swapping subviews within a container view, as well as flow mechanics, allowing supported
 * container views to be largely declarative.
 *
 * This takes care of saving the swapped out view state into its screen, so that the view state can
 * be restored when coming back in the flow. That backstack view state will also be preserved when
 * the activity is saved (e.g. on rotation or when pressing home).
 */
public abstract class ScreenSwitcher {
  public static final class Factory {
    private final int tagKey;
    private final ScreenContextFactory contextFactory;

    public Factory(int tagKey, ScreenContextFactory contextFactory) {
      this.tagKey = tagKey;
      this.contextFactory = contextFactory;
    }

    public ScreenSwitcher createScreenSwitcher(ScreenSwitcherView view) {
      return new SimpleSwitcher(view, tagKey, contextFactory);
    }
  }

  /** Set on the container view to make screen information available during a transition. */
  private static final class Tag {
    private Screen fromScreen;
    private Screen toScreen;

    public void setNextScreen(Screen screen) {
      this.fromScreen = this.toScreen;
      this.toScreen = screen;
    }
  }

  private static final Map<Class, Integer> screenLayoutCache = new LinkedHashMap<>();

  protected final LruCache<String, ViewGroup> viewCache = new LruCache<String, ViewGroup>(3) {
    @Override protected void entryRemoved(boolean evicted, String key, ViewGroup oldValue,
        ViewGroup newValue) {
      super.entryRemoved(evicted, key, oldValue, newValue);
      destroyChild(oldValue);
    }
  };

  private final ScreenSwitcherView view;
  protected final int tagKey;

  protected ScreenSwitcher(ScreenSwitcherView view, int tagKey) {
    this.view = view;
    this.tagKey = tagKey;
  }

  public void showScreen(Screen screen, CanShowScreen.Listener listener) {
    View oldChild = view.getCurrentChild();
    Screen oldChildScreen = null;

    // See if we already have the direct child we want, and if so delegate the transition.
    if (oldChild != null) {
      Tag tag = (Tag) view.getContainerView().getTag(tagKey);
      oldChildScreen = checkNotNull(tag.toScreen, "Container view has child %s with no screen",
          oldChild.toString());
      if (oldChildScreen.getName().equals(screen.getName())) {
        listener.screenShown();
        return;
      }
    }
    transition(view.getContainerView(), oldChildScreen, screen, listener);
  }

  public void destroy() {
    // Evicting children from the cache results in their destruction.
    viewCache.evictAll();
  }

  abstract void transition(ViewGroup container, Screen from, Screen to,
      CanShowScreen.Listener listener);

  abstract void destroyChild(ViewGroup child);

  protected Tag ensureTag(ViewGroup container) {
    Tag tag = (Tag) container.getTag(tagKey);
    if (tag == null) {
      tag = new Tag();
      container.setTag(tagKey, tag);
    }
    return tag;
  }

  private static class SimpleSwitcher extends ScreenSwitcher {
    private final ScreenContextFactory contextFactory;

    SimpleSwitcher(ScreenSwitcherView view, int tagKey, ScreenContextFactory contextFactory) {
      super(view, tagKey);
      this.contextFactory = contextFactory;
    }

    @Override void transition(ViewGroup container, Screen from, Screen to,
        CanShowScreen.Listener listener) {
      ViewGroup view = viewCache.get(to.getName());
      Tag tag = ensureTag(container);
      final PathContext context;
      final PathContext oldPath;
      if (container.getChildCount() > 0) {
        oldPath = PathContext.get(container.getChildAt(0).getContext());
      } else {
        oldPath = PathContext.empty(container.getContext());
      }
      if (view == null) {
        context = PathContext.create(oldPath, to, contextFactory);
        int layout = getLayout(to);
        view = (ViewGroup) LayoutInflater.from(context)
            .cloneInContext(context)
            .inflate(layout, container, false);
        // TODO: restore view caching or delete all of the code for it.
        //viewCache.put(directChild.getName(), view);
      } else {
        context = PathContext.get(view.getContext());
      }

      tag.setNextScreen(to);
      if (tag.fromScreen != null) {
        View fromView = container.getChildAt(0);
        SparseArray<Parcelable> state = new SparseArray<>();
        fromView.saveHierarchyState(state);
        tag.fromScreen.setViewState(state);
      }
      container.removeAllViews();
      container.addView(view);
      tag.toScreen.restoreHierarchyState(container.getChildAt(0));
      oldPath.destroyNotIn(context, contextFactory);
      listener.screenShown();
    }

    void destroyChild(ViewGroup child) {
      contextFactory.destroyContext(child.getContext());
    }
  }

  private static int getLayout(Object screen) {
    Class<Object> screenType = ObjectUtils.getClass(screen);
    Integer layoutResId = screenLayoutCache.get(screenType);
    if (layoutResId == null) {
      Layout layout = screenType.getAnnotation(Layout.class);
      checkNotNull(layout, "@%s annotation not found on class %s", Layout.class.getSimpleName(),
          screenType.getName());
      layoutResId = layout.value();
      screenLayoutCache.put(screenType, layoutResId);
    }
    return layoutResId;
  }
}

package com.example.flow.screenswitcher;

import android.view.View;
import android.view.ViewGroup;
import com.example.flow.appflow.Screen;
import com.example.flow.appflow.ScreenContextFactory;
import com.example.flow.util.ObjectUtils;
import flow.Flow;
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
  public abstract static class Factory {
    protected final int tagKey;
    protected final ScreenContextFactory contextFactory;

    public Factory(int tagKey, ScreenContextFactory contextFactory) {
      this.tagKey = tagKey;
      this.contextFactory = contextFactory;
    }

    public abstract ScreenSwitcher createScreenSwitcher(ScreenSwitcherView view);
  }

  /**
   * Set on the container view to make screen information available during a transition.
   * TODO(rjrjr) Does this really belong here or down in {@link SimpleSwitcher}?
   */
  protected static final class Tag {
    public Screen fromScreen;
    public Screen toScreen;

    public void setNextScreen(Screen screen) {
      this.fromScreen = this.toScreen;
      this.toScreen = screen;
    }
  }

  private static final Map<Class, Integer> SCREEN_LAYOUT_CACHE = new LinkedHashMap<>();

  private final ScreenSwitcherView view;
  protected final int tagKey;

  protected ScreenSwitcher(ScreenSwitcherView view, int tagKey) {
    this.view = view;
    this.tagKey = tagKey;
  }

  public void showScreen(Screen screen, Flow.Direction direction, final Flow.Callback callback) {
    final View oldChild = view.getCurrentChild();
    Screen oldChildScreen = null;

    // See if we already have the direct child we want, and if so delegate the transition.
    if (oldChild != null) {
      Tag tag = (Tag) view.getContainerView().getTag(tagKey);
      oldChildScreen = checkNotNull(tag.toScreen, "Container view has child %s with no screen",
          oldChild.toString());
      if (oldChildScreen.getName().equals(screen.getName())) {
        callback.onComplete();
        return;
      }
    }

    transition(view.getContainerView(), oldChildScreen, screen, direction, callback);
  }

  protected abstract void transition(ViewGroup container, Screen from, Screen to,
      Flow.Direction direction, Flow.Callback callback);

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

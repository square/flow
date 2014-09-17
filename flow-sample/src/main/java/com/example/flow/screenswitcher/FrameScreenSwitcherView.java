package com.example.flow.screenswitcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.example.flow.R;
import com.example.flow.appflow.AppFlow;
import com.example.flow.appflow.AppFlowContextFactory;
import com.example.flow.appflow.Screen;
import com.example.flow.util.ObjectUtils;

/** A FrameLayout that can show screens for an {@link AppFlow} */
public class FrameScreenSwitcherView extends FrameLayout
    implements HandlesBack, HandlesUp, ScreenSwitcherView {
  private final ScreenSwitcher container;
  private final UpAndBackHandler upAndBackHandler;

  public FrameScreenSwitcherView(Context context, AttributeSet attrs) {
    this(context, attrs,
        new ScreenSwitcher.Factory(R.id.screen_switcher_tag, new AppFlowContextFactory()));
  }

  protected FrameScreenSwitcherView(Context context, AttributeSet attrs,
      ScreenSwitcher.Factory switcherFactory) {
    super(context, attrs);
    container = switcherFactory.createScreenSwitcher(this);
    upAndBackHandler = new UpAndBackHandler(AppFlow.get(context));
  }

  @Override public ViewGroup getContainerView() {
    return this;
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
  }

  @Override public void showScreen(Screen screen) {
    showScreen(screen, new Listener() {
      @Override public void screenShown() {
      }
    });
  }

  @Override public void showScreen(Screen screen, Listener listener) {
    String tag = ObjectUtils.getClass(this).getSimpleName();
    container.showScreen(screen, listener);
  }

  @Override public boolean onUpPressed() {
    return upAndBackHandler.onUpPressed(getCurrentChild());
  }

  @Override public boolean onBackPressed() {
    return upAndBackHandler.onBackPressed(getCurrentChild());
  }

  @Override public void destroy() {
    container.destroy();
  }

  @Override public ViewGroup getCurrentChild() {
    return (ViewGroup) getContainerView().getChildAt(0);
  }
}

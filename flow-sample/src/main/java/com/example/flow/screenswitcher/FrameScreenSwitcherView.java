package com.example.flow.screenswitcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.example.flow.R;
import com.example.flow.appflow.AppFlow;
import com.example.flow.appflow.AppFlowContextFactory;
import com.example.flow.appflow.Screen;
import flow.Flow;

/** A FrameLayout that can show screens for an {@link AppFlow}. */
public class FrameScreenSwitcherView extends FrameLayout
    implements HandlesBack, HandlesUp, ScreenSwitcherView {
  private final ScreenSwitcher container;
  private final UpAndBackHandler upAndBackHandler;

  private boolean disabled;

  @SuppressWarnings("UnusedDeclaration") // Used by layout inflation, of course!
  public FrameScreenSwitcherView(Context context, AttributeSet attrs) {
    this(context, attrs,
        new SimpleSwitcher.Factory(R.id.screen_switcher_tag, new AppFlowContextFactory()));
  }

  /**
   * Allows subclasses to use custom {@link ScreenSwitcher} implementations. Allows the use
   * of more sophisticated transition schemes, and customized context wrappers.
   */
  protected FrameScreenSwitcherView(Context context, AttributeSet attrs,
      ScreenSwitcher.Factory switcherFactory) {
    super(context, attrs);
    container = switcherFactory.createScreenSwitcher(this);
    upAndBackHandler = new UpAndBackHandler(AppFlow.get(context));
  }

  @Override public boolean dispatchTouchEvent(MotionEvent ev) {
    return !disabled && super.dispatchTouchEvent(ev);
  }

  @Override public ViewGroup getContainerView() {
    return this;
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
  }

  @Override public void showScreen(Screen screen, Flow.Direction direction,
      final Flow.Callback callback) {
    disabled = true;
    container.showScreen(screen, direction, new Flow.Callback() {
      @Override public void onComplete() {
        callback.onComplete();
        disabled = false;
      }
    });
  }

  @Override public boolean onUpPressed() {
    return upAndBackHandler.onUpPressed(getCurrentChild());
  }

  @Override public boolean onBackPressed() {
    return upAndBackHandler.onBackPressed(getCurrentChild());
  }

  @Override public ViewGroup getCurrentChild() {
    return (ViewGroup) getContainerView().getChildAt(0);
  }
}

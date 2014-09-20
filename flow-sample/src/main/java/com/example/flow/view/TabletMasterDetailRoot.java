package com.example.flow.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import com.example.flow.MainActivity;
import com.example.flow.R;
import com.example.flow.Screens;
import com.example.flow.appflow.AppFlow;
import com.example.flow.appflow.Screen;
import com.example.flow.screenswitcher.CanShowScreen;
import com.example.flow.screenswitcher.FrameScreenSwitcherView;
import com.example.flow.screenswitcher.HandlesBack;
import com.example.flow.screenswitcher.HandlesUp;
import com.example.flow.screenswitcher.UpAndBackHandler;
import flow.Flow;
import java.util.concurrent.CountDownLatch;

/**
 * This view is shown only in landscape orientation on tablets. See
 * the explanation in {@link MainActivity#onCreate}.
 */
public class TabletMasterDetailRoot extends LinearLayout
    implements HandlesBack, HandlesUp, CanShowScreen {
  private final UpAndBackHandler upAndBackHandler;

  private FrameScreenSwitcherView masterContainer;
  private FrameScreenSwitcherView detailContainer;

  private boolean disabled;

  public TabletMasterDetailRoot(Context context, AttributeSet attrs) {
    super(context, attrs);
    upAndBackHandler = new UpAndBackHandler(AppFlow.get(context));
  }

  @Override public boolean dispatchTouchEvent(MotionEvent ev) {
    return !disabled && super.dispatchTouchEvent(ev);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    masterContainer = (FrameScreenSwitcherView) findViewById(R.id.master);
    detailContainer = (FrameScreenSwitcherView) findViewById(R.id.detail);
  }

  /**
   * Updates both {@link #masterContainer} and {@link #detailContainer} to reflect
   * the current screen. If this is a detail screen, it is shown on the right and its {@link
   * Screens.MasterDetailScreen#getMaster() master} on the left. If it is a master screen,
   * it is shown on the left and {@link Screens.NoDetails} on the right.
   * <p>
   * Note this interesting implementation detail: the contract here is that that the given
   * {@link Flow.Callback} must be fired when any transition animation is complete. Since this
   * is a composite view wrapping two other {@link CanShowScreen} instances, we use a
   * {@link CountDownLatch} to fire it only when both containers report ready.
   *
   * @param screen must be an instance of {@link Screens.MasterDetailScreen}
   */
  @Override public void showScreen(Screen screen, Flow.Direction direction,
      Flow.Callback callback) {

    class LatchedCallback implements Flow.Callback {
      final Flow.Callback wrapped;
      final CountDownLatch latch = new CountDownLatch(2);

      LatchedCallback(Flow.Callback wrapped) {
        this.wrapped = wrapped;
      }

      @Override public void onComplete() {
        latch.countDown();
        if (latch.getCount() == 0) {
          disabled = false;
          wrapped.onComplete();
          ((IsMasterView) masterContainer.getCurrentChild()).updateSelection();
        }
      }
    }

    disabled = true;

    Screens.MasterDetailScreen masterDetailScreen = (Screens.MasterDetailScreen) screen;
    Screen masterScreen;
    Screen detailScreen;

    if (masterDetailScreen.isMaster()) {
      masterScreen = masterDetailScreen;
      detailScreen = new Screens.NoDetails();
    } else {
      masterScreen = masterDetailScreen.getMaster();
      detailScreen = masterDetailScreen;
    }

    callback = new LatchedCallback(callback);
    detailContainer.showScreen(detailScreen, direction, callback);
    masterContainer.showScreen(masterScreen, direction, callback);
  }

  @Override public boolean onUpPressed() {
    return upAndBackHandler.onUpPressed(detailContainer);
  }

  @Override public boolean onBackPressed() {
    return upAndBackHandler.onBackPressed(detailContainer);
  }
}

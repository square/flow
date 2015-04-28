package com.example.flow.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.example.flow.Paths;
import com.example.flow.R;
import com.example.flow.pathview.BackSupport;
import com.example.flow.pathview.FramePathContainerView;
import com.example.flow.pathview.HandlesBack;
import flow.Flow;
import flow.path.Path;
import flow.path.PathContainerView;

/**
 * This view is shown only in landscape orientation on tablets. See
 * the explanation in {@link com.example.flow.MainActivity#onCreate}.
 */
public class TabletMasterDetailRoot extends LinearLayout implements HandlesBack, PathContainerView {
  private FramePathContainerView masterContainer;
  private FramePathContainerView detailContainer;

  private boolean disabled;

  public TabletMasterDetailRoot(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override public boolean dispatchTouchEvent(MotionEvent ev) {
    return !disabled && super.dispatchTouchEvent(ev);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    masterContainer = (FramePathContainerView) findViewById(R.id.master);
    detailContainer = (FramePathContainerView) findViewById(R.id.detail);
  }

  @Override public ViewGroup getCurrentChild() {
    Paths.MasterDetailPath showing = Path.get(getContext());
    return showing.isMaster() ? masterContainer.getCurrentChild()
        : detailContainer.getCurrentChild();
  }

  @Override public ViewGroup getContainerView() {
    return this;
  }

  @Override public void dispatch(final Flow.Traversal traversal, Flow.TraversalCallback callback) {

    class CountdownCallback implements Flow.TraversalCallback {
      final Flow.TraversalCallback wrapped;
      int countDown = 2;

      CountdownCallback(Flow.TraversalCallback wrapped) {
        this.wrapped = wrapped;
      }

      @Override public void onTraversalCompleted() {
        countDown--;
        if (countDown == 0) {
          disabled = false;
          wrapped.onTraversalCompleted();
          ((IsMasterView) masterContainer.getCurrentChild()).updateSelection(
              traversal.destination.<Paths.MasterDetailPath>top());
        }
      }
    }

    disabled = true;
    callback = new CountdownCallback(callback);
    detailContainer.dispatch(traversal, callback);
    masterContainer.dispatch(traversal, callback);
  }

  @Override public boolean onBackPressed() {
    return BackSupport.onBackPressed(detailContainer);
  }
}

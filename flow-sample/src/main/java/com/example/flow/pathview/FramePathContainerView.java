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

package com.example.flow.pathview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.example.flow.R;
import flow.Flow;
import flow.Path;
import flow.PathContainer;
import flow.PathContainerView;

/** A FrameLayout that can show screens for a {@link flow.Flow}. */
public class FramePathContainerView extends FrameLayout
    implements HandlesBack, HandlesUp, PathContainerView {
  private final PathContainer container;
  private final UpAndBackHandler upAndBackHandler;

  private boolean disabled;

  @SuppressWarnings("UnusedDeclaration") // Used by layout inflation, of course!
  public FramePathContainerView(Context context, AttributeSet attrs) {
    this(context, attrs,
        new SimplePathContainer.Factory(R.id.screen_switcher_tag, Path.contextFactory()));
  }

  /**
   * Allows subclasses to use custom {@link flow.PathContainer} implementations. Allows the use
   * of more sophisticated transition schemes, and customized context wrappers.
   */
  protected FramePathContainerView(Context context, AttributeSet attrs,
      PathContainer.Factory switcherFactory) {
    super(context, attrs);
    container = switcherFactory.createPathContainer(this);
    upAndBackHandler = new UpAndBackHandler(Flow.get(context));
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

  @Override public void executeTraversal(Flow.Traversal traversal,
      final Flow.TraversalCallback callback) {
    disabled = true;
    container.executeTraversal(traversal, new Flow.TraversalCallback() {
      @Override public void onTraversalCompleted() {
        callback.onTraversalCompleted();
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

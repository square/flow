/*
 * Copyright 2013 Square Inc.
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

package com.example.flow.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.example.flow.Utils;
import com.squareup.flow.Flow;

public class ContainerView extends FrameLayout {
  private boolean disabled;

  private View activeView;

  public ContainerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override public boolean dispatchTouchEvent(MotionEvent ev) {
    return !disabled && super.dispatchTouchEvent(ev);
  }

  public void displayView(View view, final Flow.Direction direction) {
    addView(view);

    if (activeView != null) {
      final View from = activeView;
      final View to = view;
      Utils.waitForMeasure(to, new Utils.OnMeasuredCallback() {
        @Override public void onMeasured(View view, int width, int height) {
          runAnimation(from, to, direction);
        }
      });
    }

    activeView = view;
  }

  private void runAnimation(final View from, final View to, Flow.Direction direction) {
    disabled = true;

    Animator animator = createSegue(from, to, direction);
    animator.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator animation) {
        removeView(from);
        disabled = false;
      }
    });
    animator.start();
  }

  private Animator createSegue(View from, View to, Flow.Direction direction) {
    boolean backward = direction == Flow.Direction.BACKWARD;
    int fromTranslation = backward ? from.getWidth() : -from.getWidth();
    int toTranslation = backward ? -to.getWidth() : to.getWidth();

    AnimatorSet set = new AnimatorSet();

    set.play(ObjectAnimator.ofFloat(from, View.TRANSLATION_X, fromTranslation));
    set.play(ObjectAnimator.ofFloat(to, View.TRANSLATION_X, toTranslation, 0));

    return set;
  }
}

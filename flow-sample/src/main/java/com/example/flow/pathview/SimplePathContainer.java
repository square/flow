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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.flow.util.Utils;
import flow.Flow;
import flow.Path;
import flow.PathContainer;
import flow.PathContainerView;
import flow.PathContext;
import flow.PathContextFactory;

import static flow.Flow.Direction.REPLACE;

/**
 * Provides basic right-to-left transitions. Saves and restores view state.
 * Uses {@link flow.PathContext} to allow customized sub-containers.
 */
public class SimplePathContainer extends PathContainer {
  public static final class Factory extends PathContainer.Factory {
    public Factory(int tagKey, PathContextFactory contextFactory) {
      super(tagKey, contextFactory);
    }

    @Override public PathContainer createPathContainer(PathContainerView view) {
      return new SimplePathContainer(view, tagKey, contextFactory);
    }
  }

  private final PathContextFactory contextFactory;

  SimplePathContainer(PathContainerView view, int tagKey, PathContextFactory contextFactory) {
    super(view, tagKey);
    this.contextFactory = contextFactory;
  }

  @Override protected void performTraversal(final ViewGroup containerView,
      final TraversalState traversalState, final Flow.Direction direction,
      final Flow.TraversalCallback callback) {

    final PathContext context;
    final PathContext oldPath;
    if (containerView.getChildCount() > 0) {
      oldPath = PathContext.get(containerView.getChildAt(0).getContext());
    } else {
      oldPath = PathContext.root(containerView.getContext());
    }

    Path to = traversalState.toPath();

    ViewGroup newView;
    context = PathContext.create(oldPath, to, contextFactory);
    int layout = getLayout(to);
    newView = (ViewGroup) LayoutInflater.from(context)
        .cloneInContext(context)
        .inflate(layout, containerView, false);

    View fromView = null;
    if (traversalState.fromPath() != null) {
      fromView = containerView.getChildAt(0);
      traversalState.saveViewState(fromView);
    }
    traversalState.restoreViewState(newView);

    if (fromView == null || direction == REPLACE) {
      containerView.removeAllViews();
      containerView.addView(newView);
      oldPath.destroyNotIn(context, contextFactory);
      callback.onTraversalCompleted();
    } else {
      containerView.addView(newView);
      final View finalFromView = fromView;
      Utils.waitForMeasure(newView, new Utils.OnMeasuredCallback() {
        @Override public void onMeasured(View view, int width, int height) {
          runAnimation(containerView, finalFromView, view, direction, new Flow.TraversalCallback() {
            @Override public void onTraversalCompleted() {
              containerView.removeView(finalFromView);
              oldPath.destroyNotIn(context, contextFactory);
              callback.onTraversalCompleted();
            }
          });
        }
      });
    }
  }

  private void runAnimation(final ViewGroup container, final View from, final View to,
      Flow.Direction direction, final Flow.TraversalCallback callback) {
    Animator animator = createSegue(from, to, direction);
    animator.addListener(new AnimatorListenerAdapter() {
      @Override public void onAnimationEnd(Animator animation) {
        container.removeView(from);
        callback.onTraversalCompleted();
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

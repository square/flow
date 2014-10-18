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
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.flow.path.Path;
import com.example.flow.path.PathContext;
import com.example.flow.path.PathContextFactory;
import com.example.flow.path.PathContainer;
import com.example.flow.path.PathContainerView;
import com.example.flow.util.Utils;
import flow.Flow;

import static flow.Flow.Direction.REPLACE;

/**
 * Provides basic right-to-left transitions. Saves and restores view state.
 * Uses {@link com.example.flow.path.PathContext} to allow customized sub-containers.
 * <p>
 * TODO(rjrjr) Wouldn't it be nice if the sample app DEMONSTRATED just what the
 * hell a subcontainer is?
 */
public class SimplePathContainer extends PathContainer {
  public static final class Factory extends PathContainer.Factory {
    public Factory(int tagKey, PathContextFactory contextFactory) {
      super(tagKey, contextFactory);
    }

    @Override public PathContainer createPathController(PathContainerView view) {
      return new SimplePathContainer(view, tagKey, contextFactory);
    }
  }

  private final PathContextFactory contextFactory;

  SimplePathContainer(PathContainerView view, int tagKey, PathContextFactory contextFactory) {
    super(view, tagKey);
    this.contextFactory = contextFactory;
  }

  @Override protected void transition(final ViewGroup container, Path from, Path to,
      final Flow.Direction direction, final Flow.TraversalCallback callback) {
    final Tag tag = ensureTag(container);
    final PathContext context;
    final PathContext oldPath;
    if (container.getChildCount() > 0) {
      oldPath = PathContext.get(container.getChildAt(0).getContext());
    } else {
      oldPath = PathContext.root(container.getContext());
    }

    ViewGroup view;
    context = PathContext.create(oldPath, to, contextFactory);
    int layout = getLayout(to);
    view = (ViewGroup) LayoutInflater.from(context)
        .cloneInContext(context)
        .inflate(layout, container, false);

    View fromView = null;
    tag.setNextScreen(to);
    if (tag.fromPath != null) {
      fromView = container.getChildAt(0);
      SparseArray<Parcelable> state = new SparseArray<>();
      fromView.saveHierarchyState(state);
      tag.fromPath.setViewState(state);
    }

    if (fromView == null || direction == REPLACE) {
      container.removeAllViews();
      container.addView(view);
      tag.toPath.restoreHierarchyState(container.getChildAt(0));
      oldPath.destroyNotIn(context, contextFactory);
      callback.onTraversalCompleted();
    } else {
      container.addView(view);
      final View finalFromView = fromView;
      Utils.waitForMeasure(view, new Utils.OnMeasuredCallback() {
        @Override public void onMeasured(View view, int width, int height) {
          runAnimation(container, finalFromView, view, direction, new Flow.TraversalCallback() {
            @Override public void onTraversalCompleted() {
              container.removeView(finalFromView);
              tag.toPath.restoreHierarchyState(container.getChildAt(0));
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

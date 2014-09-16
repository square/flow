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

package com.example.flow.util;

import android.content.Context;
import android.view.View;
import android.view.ViewTreeObserver;
import com.example.flow.DemoApp;

public final class Utils {
  public static void inject(Context context, Object thing) {
    ((DemoApp) context.getApplicationContext()).getGlobalGraph().inject(thing);
  }

  public interface OnMeasuredCallback {
    void onMeasured(View view, int width, int height);
  }

  public static void waitForMeasure(final View view, final OnMeasuredCallback callback) {
    int width = view.getWidth();
    int height = view.getHeight();

    if (width > 0 && height > 0) {
      callback.onMeasured(view, width, height);
      return;
    }

    view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
      @Override public boolean onPreDraw() {
        final ViewTreeObserver observer = view.getViewTreeObserver();
        if (observer.isAlive()) {
          observer.removeOnPreDrawListener(this);
        }

        callback.onMeasured(view, view.getWidth(), view.getHeight());

        return true;
      }
    });
  }

  private Utils() {
  }
}

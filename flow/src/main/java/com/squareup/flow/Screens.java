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

package com.squareup.flow;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import com.squareup.flow.annotation.Layout;
import com.squareup.flow.annotation.View;
import java.lang.reflect.Constructor;

public final class Screens {
  private static final Class<?>[] VIEW_CONSTRUCTOR = new Class[] {
      Context.class, AttributeSet.class
  };

  /** Create an instance of the view specified in a {@link View} or {@link Layout} annotation. */
  public static android.view.View createView(Context context, Screen screen) {
    return createView(context, screen.getClass());
  }

  /** Create an instance of the view specified in a {@link View} or {@link Layout} annotation. */
  public static android.view.View createView(Context context, Class<? extends Screen> screenType) {
    View view = screenType.getAnnotation(View.class);
    if (view != null) {
      return instantiateView(context, view.value());
    }

    Layout layout = screenType.getAnnotation(Layout.class);
    if (layout != null) {
      return inflateLayout(context, layout.value());
    }

    throw new IllegalArgumentException(
        screenType + " does not have either a @View or @Layout annotation");
  }

  private static android.view.View inflateLayout(Context context, int layoutId) {
    return LayoutInflater.from(context).inflate(layoutId, null);
  }

  private static android.view.View instantiateView(Context context,
      Class<? extends android.view.View> type) {
    try {
      Constructor<? extends android.view.View> constructor = type.getConstructor(VIEW_CONSTRUCTOR);
      return constructor.newInstance(context, null);
    } catch (Exception e) {
      throw new IllegalStateException("View could not be created", e);
    }
  }

  private Screens() {
  }
}

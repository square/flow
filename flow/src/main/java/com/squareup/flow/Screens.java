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
import android.view.View;
import java.lang.reflect.Constructor;

public final class Screens {
  private static final Class<?>[] VIEW_CONSTRUCTOR = new Class[] {
      Context.class, AttributeSet.class
  };

  /** Create an instance of the view specified in a {@link Screen} annotation. */
  public static android.view.View createView(Context context, Object screen) {
    return createView(context, screen.getClass());
  }

  /** Create an instance of the view specified in a {@link Screen} annotation. */
  public static android.view.View createView(Context context, Class<?> screenType) {
    Screen screen = screenType.getAnnotation(Screen.class);
    if (screen == null) {
      throw new IllegalArgumentException(
          String.format("@%s annotation not found on class %s", Screen.class.getSimpleName(),
              screenType.getName()));
    }

    int layout = screen.layout();
    if (layout != View.NO_ID) return inflateLayout(context, layout);

    return instantiateView(context, screen.value());
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

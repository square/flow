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

package flow;

import android.content.Context;
import android.view.LayoutInflater;

public final class Layouts {

  /** Create an instance of the view specified in a {@link Layout} annotation. */
  public static android.view.View createView(Context context, Object screen) {
    return createView(context, screen.getClass());
  }

  /** Create an instance of the view specified in a {@link Layout} annotation. */
  public static android.view.View createView(Context context, Class<?> screenType) {
    Layout screen = screenType.getAnnotation(Layout.class);
    if (screen == null) {
      throw new IllegalArgumentException(
          String.format("@%s annotation not found on class %s", Layout.class.getSimpleName(),
              screenType.getName()));
    }

    int layout = screen.value();
    return inflateLayout(context, layout);
  }

  private static android.view.View inflateLayout(Context context, int layoutId) {
    return LayoutInflater.from(context).inflate(layoutId, null);
  }

  private Layouts() {
  }
}

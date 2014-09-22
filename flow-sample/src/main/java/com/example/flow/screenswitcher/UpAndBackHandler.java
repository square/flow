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

package com.example.flow.screenswitcher;

import android.view.View;
import flow.Flow;

/**
 * Support for {@link HandlesUp} and {@link HandlesBack}.
 */
public class UpAndBackHandler {
  private final Flow flow;

  public UpAndBackHandler(Flow flow) {
    this.flow = flow;
  }

  public boolean onUpPressed(View childView) {
    if (childView instanceof HandlesUp) {
      if (((HandlesUp) childView).onUpPressed()) {
        return true;
      }
    }
    // Try to go up.  If up isn't supported, go back.
    return flow.goUp() || onBackPressed(childView);
  }

  public boolean onBackPressed(View childView) {
    if (childView instanceof HandlesBack) {
      if (((HandlesBack) childView).onBackPressed()) {
        return true;
      }
    }
    return flow.goBack();
  }
}

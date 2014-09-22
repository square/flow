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

package com.example.flow.appflow;

import android.content.Context;
import javax.annotation.Nullable;

public final class AppFlowContextFactory implements ScreenContextFactory {
  @Nullable private final ScreenContextFactory delegate;

  public AppFlowContextFactory() {
    delegate = null;
  }

  public AppFlowContextFactory(ScreenContextFactory delegate) {
    this.delegate = delegate;
  }

  @Override public Context createContext(Screen screen, Context parentContext) {
    if (delegate != null) {
      parentContext = delegate.createContext(screen, parentContext);
    }
    return AppFlow.setScreen(parentContext, screen);
  }

  @Override public void destroyContext(Context context) {
    if (delegate != null) {
      delegate.destroyContext(context);
    }
  }
}

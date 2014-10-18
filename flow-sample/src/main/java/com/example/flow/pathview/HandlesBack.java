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

/**
 * Implemented by views that want the option to intercept back button taps. If a view has subviews
 * that implement this interface their {@link #onBackPressed()} method should be invoked before
 * any of this view's own logic.
 * <p/>
 *
 * The typical flow of back button handling starts in the {@link android.app.Activity#onBackPressed()}
 * calling {@link #onBackPressed()} on its content view. Each view in turn delegates to its
 * child views to give them first say.
 */
public interface HandlesBack {
  /**
   * Returns <code>true</code> if back event was handled, <code>false</code> if someone higher in
   * the chain should.
   */
  boolean onBackPressed();
}

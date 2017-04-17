/*
 * Copyright 2016 Square Inc.
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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.annotation.Nullable;

final class InternalContextWrapper extends ContextWrapper {
  private static final String FLOW_SERVICE = "flow.InternalContextWrapper.FLOW_SERVICE";
  private static final String CONTEXT_MANAGER_SERVICE =
      "flow.InternalContextWrapper.CONTEXT_MANAGER_SERVICE";

  @Nullable static Flow getFlow(Context context) {
    @SuppressWarnings("WrongConstant")
    Flow systemService = (Flow) context.getSystemService(FLOW_SERVICE);
    return systemService;
  }

  static KeyManager getContextManager(Context context) {
    @SuppressWarnings("WrongConstant")
    final KeyManager service =
        (KeyManager) context.getSystemService(CONTEXT_MANAGER_SERVICE);
    return service;
  }

  private final Activity activity;
  private Flow flow;
  private KeyManager keyManager;

  InternalContextWrapper(Context baseContext, Activity activity) {
    super(baseContext);
    this.activity = activity;
  }

  @Override public Object getSystemService(String name) {
    if (FLOW_SERVICE.equals(name)) {
      if (flow == null) {
        flow = InternalLifecycleIntegration.require(activity).flow;
      }
      return flow;
    } else if (CONTEXT_MANAGER_SERVICE.equals(name)) {
      if (keyManager == null) {
        keyManager = InternalLifecycleIntegration.require(activity).keyManager;
      }
      return keyManager;
    }
    return super.getSystemService(name);
  }
}

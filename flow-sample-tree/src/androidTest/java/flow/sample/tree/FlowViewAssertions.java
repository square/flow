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

package flow.sample.tree;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.view.View;
import flow.Flow;

final class FlowViewAssertions {
  private FlowViewAssertions() {
    // No instances.
  }

  static ViewAssertion doesNotHaveFlowService(final String serviceName) {
    return new FlowServiceIsNull(serviceName, true);
  }

  static ViewAssertion hasFlowService(final String serviceName) {
    return new FlowServiceIsNull(serviceName, false);
  }

  private static final class FlowServiceIsNull implements ViewAssertion {
    private final String serviceName;
    private final boolean isNull;

    public FlowServiceIsNull(String serviceName, boolean isNull) {
      this.serviceName = serviceName;
      this.isNull = isNull;
    }

    @Override public void check(View view, NoMatchingViewException noViewFoundException) {
      if ((Flow.getService(serviceName, view.getContext()) == null) != isNull) {
        throw new AssertionError();
      }
    }
  }
}

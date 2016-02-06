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
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.widget.TextView;
import java.util.Map;

final class DefaultKeyChanger extends KeyChanger {
  private Activity activity;
  private TextView textView;

  DefaultKeyChanger(Activity activity) {
    this.activity = activity;
  }

  @Override public void changeKey(@Nullable State outgoingState, State incomingState,
      Flow.Direction direction, Map<Object, Context> incomingContexts,
      Flow.TraversalCallback callback) {
    if (textView == null) {
      textView = new TextView(incomingContexts.get(incomingState.getKey()));
      textView.setGravity(Gravity.CENTER);
      activity.setContentView(textView);
    }
    textView.setText(incomingState.getKey().toString());
    callback.onTraversalCompleted();
  }
}

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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import java.util.Map;

public interface KeyChanger {
  /**
   * Transition from outgoing state to incoming state.  Implementations should call
   * {@link State#restore(View)} on the incoming view, and (if outgoingState is not null)
   * {@link State#save(View)} on the outgoing view.  And don't forget to declare your screen layouts
   * with ids (only layouts with ids will have their state saved/restored)!
   */
  void changeKey(@Nullable State outgoingState, @NonNull State incomingState,
      @NonNull Direction direction, @NonNull Map<Object, Context> incomingContexts,
      @NonNull TraversalCallback callback);
}

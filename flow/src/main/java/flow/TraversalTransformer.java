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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static flow.Preconditions.checkNotNull;

public interface TraversalTransformer {

  class Transform {
    @NonNull public final History destination;
    @NonNull public final Direction direction;

    Transform(@NonNull History destination, @NonNull Direction direction) {
      checkNotNull(destination, "Destination must not be null!");
      checkNotNull(direction, "Direction must not be null!");
      this.destination = destination;
      this.direction = direction;
    }
  }

  /**
   * The given {@code pendingTraversal} is about to be given to the Dispatcher, either return null
   * to proceed unaltered or return a {@link Transform} with a revised destination history and
   * direction to alter the impending dispatch.
   * <p>
   * Particularly useful for redirects or transformations of one key into multiple.
   */
  @Nullable Transform maybeTransform(Traversal pendingTraversal);
}

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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public final class StateTest {
  @Test public void disallowDuplicateViewIds() {
    Activity activity = Robolectric.setupActivity(Activity.class);
    State state = State.fromBundle(new Bundle(), new KeyParceler() {
      @NonNull @Override public Parcelable toParcelable(@NonNull Object key) {
        return (Parcelable) key;
      }

      @NonNull @Override public Object toKey(@NonNull Parcelable parcelable) {
        return parcelable;
      }
    });
    int id = 1;
    FrameLayout parent = new FrameLayout(activity);
    parent.setId(id);
    View child = new View(activity);
    child.setId(id);
    parent.addView(child);
    try {
      state.save(parent);
      fail();
    } catch (IllegalStateException expected) {
      assertEquals("Duplicate View id in current tree being saved: 1", expected.getMessage());
    }
  }
}

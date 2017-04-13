/*
 * Copyright 2017 Square Inc.
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

package flow.sample.intents;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import flow.KeyParceler;

public class StringParceler implements KeyParceler {
  @NonNull @Override public Parcelable toParcelable(@NonNull Object key) {
    Bundle bundle = new Bundle();
    bundle.putString("stringKey", (String) key);
    return bundle;
  }

  @NonNull @Override public Object toKey(@NonNull Parcelable parcelable) {
    Bundle bundle = (Bundle) parcelable;
    return bundle.getString("stringKey");
  }
}

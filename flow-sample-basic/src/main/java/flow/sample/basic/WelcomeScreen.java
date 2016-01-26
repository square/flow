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

package flow.sample.basic;

import android.os.Parcel;
import android.os.Parcelable;

final class WelcomeScreen implements Parcelable {
  @Override public void writeToParcel(Parcel dest, int flags) {
  }

  @Override public int describeContents() {
    return 0;
  }

  public static final Creator<WelcomeScreen> CREATOR = new Creator<WelcomeScreen>() {
    @Override public WelcomeScreen createFromParcel(Parcel in) {
      return new WelcomeScreen();
    }

    @Override public WelcomeScreen[] newArray(int size) {
      return new WelcomeScreen[size];
    }
  };

  @Override public boolean equals(Object o) {
    return o != null && o instanceof WelcomeScreen;
  }
}

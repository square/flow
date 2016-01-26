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

final class HelloScreen implements Parcelable {
  final String name;

  HelloScreen(String name) {
    this.name = name;
  }

  protected HelloScreen(Parcel in) {
    name = in.readString();
  }

  public static final Creator<HelloScreen> CREATOR = new Creator<HelloScreen>() {
    @Override public HelloScreen createFromParcel(Parcel in) {
      return new HelloScreen(in.readString());
    }

    @Override public HelloScreen[] newArray(int size) {
      return new HelloScreen[size];
    }
  };

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HelloScreen that = (HelloScreen) o;

    return name.equals(that.name);
  }

  @Override public int hashCode() {
    return name.hashCode();
  }
}

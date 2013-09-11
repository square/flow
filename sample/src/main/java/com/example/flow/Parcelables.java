/*
 * Copyright 2013 Square Inc.
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

package com.example.flow;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.reflect.Array;

public class Parcelables {
  public static <T extends Parcelable> Parcelable.Creator<T> emptyCreator(final Class<T> cl) {
    return new Parcelable.Creator<T>() {
      @Override public T createFromParcel(Parcel parcel) {
        try {
          return cl.newInstance();
        } catch (InstantiationException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }

      @SuppressWarnings("unchecked") @Override public T[] newArray(int i) {
        return (T[]) Array.newInstance(cl, i);
      }
    };
  }

  private Parcelables() {}
}

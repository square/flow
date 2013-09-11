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

package com.squareup.flow;

import android.os.Parcelable;

public interface Screen extends Parcelable {
  interface HasParent<T extends Screen> {
    T getParent();
  }
}

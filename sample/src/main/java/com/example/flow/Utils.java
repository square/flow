package com.example.flow;

import android.content.Context;
import android.view.View;

public class Utils {
  public static void inject(Context context, View view) {
    ((Injector) context).inject(view);
  }

  private Utils() {}
}

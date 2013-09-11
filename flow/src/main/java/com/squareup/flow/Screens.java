package com.squareup.flow;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import com.squareup.flow.annotation.Layout;
import com.squareup.flow.annotation.View;
import java.lang.reflect.Constructor;

public class Screens {
  private static final Class<?>[] VIEW_CONSTRUCTOR = new Class[] {
      Context.class, AttributeSet.class
  };

  public static android.view.View createView(Context context, Screen screen) {
    return createView(context, screen.getClass());
  }

  public static android.view.View createView(Context context, Class<? extends Screen> screenType) {
    View view = screenType.getAnnotation(View.class);
    if (view != null) {
      return instantiateView(context, view.value());
    }

    Layout layout = screenType.getAnnotation(Layout.class);
    if (layout != null) {
      return inflateLayout(context, layout.value());
    }

    throw new IllegalArgumentException(
        screenType + " does not have either a @View or @Layout annotation");
  }

  private static android.view.View inflateLayout(Context context, int layoutId) {
    return LayoutInflater.from(context).inflate(layoutId, null);
  }

  private static android.view.View instantiateView(Context context,
      Class<? extends android.view.View> type) {
    try {
      Constructor<? extends android.view.View> constructor = type.getConstructor(VIEW_CONSTRUCTOR);
      return constructor.newInstance(context, null);
    } catch (Exception e) {
      throw new IllegalStateException("View could not be created", e);
    }
  }

  private Screens() {
  }
}

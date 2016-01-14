package flow.sample.basic;

import android.content.Context;

final class Screens {
  private static final String SCREEN = "SERVICE_SCREEN";

  public static <T> T getScreen(Context context) {
    //noinspection unchecked,ResourceType
    return (T) context.getSystemService(SCREEN);
  }

  static final class ContextWrapper extends android.content.ContextWrapper {
    private final Object screen;

    ContextWrapper(Context base, Object screen) {
      super(base);
      this.screen = screen;
    }

    @Override public Object getSystemService(String name) {
      if (SCREEN.equals(name)) return screen;
      return super.getSystemService(name);
    }
  }
}

package com.example.flow.appflow;

import android.content.Context;

public interface ScreenContextFactory {
  Context createContext(Screen screen, Context parentContext);

  void destroyContext(Context context);
}

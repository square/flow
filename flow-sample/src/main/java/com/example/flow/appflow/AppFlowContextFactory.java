package com.example.flow.appflow;

import android.content.Context;
import javax.annotation.Nullable;

public final class AppFlowContextFactory implements ScreenContextFactory {
  @Nullable private final ScreenContextFactory delegate;

  public AppFlowContextFactory() {
    delegate = null;
  }

  public AppFlowContextFactory(ScreenContextFactory delegate) {
    this.delegate = delegate;
  }

  @Override public Context createContext(Screen screen, Context parentContext) {
    if (delegate != null) {
      parentContext = delegate.createContext(screen, parentContext);
    }
    return AppFlow.setScreen(parentContext, screen);
  }

  @Override public void destroyContext(Context context) {
    if (delegate != null) {
      delegate.destroyContext(context);
    }
  }
}

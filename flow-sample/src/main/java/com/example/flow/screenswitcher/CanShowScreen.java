package com.example.flow.screenswitcher;

import com.example.flow.appflow.Screen;

public interface CanShowScreen {
  interface Listener {
    /** Must be called when the screen has been shown, after any transition completes. */
    void screenShown();
  }

  void showScreen(Screen screen);

  void showScreen(Screen screen, Listener listener);
}

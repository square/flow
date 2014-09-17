package com.example.flow.screenswitcher;

import com.example.flow.appflow.Screen;

public interface ScreenTransitionListener {
  /** Called on the incoming View when the screen transition is done and the new view is shown. */
  void onTransitionEnd(Screen targetScreen);
}

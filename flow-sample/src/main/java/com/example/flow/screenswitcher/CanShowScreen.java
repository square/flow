package com.example.flow.screenswitcher;

import com.example.flow.appflow.Screen;
import flow.Flow;

public interface CanShowScreen {
  void showScreen(Screen screen, Flow.Direction direction, Flow.Callback callback);
}

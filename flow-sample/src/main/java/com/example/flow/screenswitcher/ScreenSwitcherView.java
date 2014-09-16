package com.example.flow.screenswitcher;

import android.content.Context;
import android.view.ViewGroup;

public interface ScreenSwitcherView extends CanShowScreen {
  ViewGroup getCurrentChild();

  ViewGroup getContainerView();

  Context getContext();
}

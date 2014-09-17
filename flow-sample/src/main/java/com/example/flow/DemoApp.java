package com.example.flow;

import android.app.Application;
import dagger.ObjectGraph;

public class DemoApp extends Application {
  private ObjectGraph globalGraph;

  @Override public void onCreate() {
    super.onCreate();

    globalGraph = ObjectGraph.create(new DaggerConfig());
  }

  public ObjectGraph getGlobalGraph() {
    return globalGraph;
  }
}

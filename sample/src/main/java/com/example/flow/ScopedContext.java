package com.example.flow;

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import dagger.ObjectGraph;

public class ScopedContext extends ContextWrapper implements Injector {
  public final ObjectGraph graph;

  private LayoutInflater inflater;

  public ScopedContext(Context context, ObjectGraph graph) {
    super(context);

    this.graph = graph;
  }

  @Override public <T> T get(Class<? extends T> type) {
    return graph.get(type);
  }

  @Override public <T> void inject(T instance) {
    graph.inject(instance);
  }

  @Override public Object getSystemService(String name) {
    if (LAYOUT_INFLATER_SERVICE.equals(name)) {
      if (inflater == null) {
        inflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
      }
      return inflater;
    }

    return super.getSystemService(name);
  }
}

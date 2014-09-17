package com.example.flow.screenswitcher;

import android.content.Context;
import android.content.ContextWrapper;
import com.example.flow.appflow.Screen;
import com.example.flow.appflow.ScreenContextFactory;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.flow.util.Preconditions.checkArgument;
import static com.example.flow.util.Preconditions.checkNotNull;

public class PathContext extends ContextWrapper {
  private static final String SERVICE_NAME = "PATH_CONTEXT";
  private static final List<Screen> EMPTY_PATH = Collections.emptyList();
  private static final Map<String, Context> EMPTY_CONTEXT_MAP = Collections.emptyMap();
  private final List<Screen> path;
  private final Map<String, Context> contexts;

  PathContext(Context leafContext, List<Screen> path, Map<String, Context> contexts) {
    super(leafContext);
    checkArgument(leafContext != null, "Leaf context may not be null.");
    checkArgument(path.size() == contexts.size(), "Path and context map are not the same size");
    if (!path.isEmpty()) {
      Screen leafScreen = path.get(path.size() - 1);
      checkArgument(leafContext == contexts.get(leafScreen.getName()),
          "Base context is not path's leaf context.");
    }
    this.path = path;
    this.contexts = contexts;
  }

  public static PathContext empty(Context baseContext) {
    return new PathContext(baseContext, EMPTY_PATH, EMPTY_CONTEXT_MAP);
  }

  public static PathContext create(PathContext basePath, Screen screen,
      ScreenContextFactory factory) {
    List<Screen> path = screen.getPath();
    Map<String, Context> contexts = new LinkedHashMap<>();
    if (path.isEmpty()) throw new IllegalArgumentException("Screen has empty path");
    Context context = basePath.getBaseContext();
    // We walk down the path, reusing cached contexts for the elements we encounter.  As soon as
    // we encounter a screen that's not in the cache, we stop.
    Iterator<Screen> pathIterator = path.iterator();
    Iterator<Screen> basePathIterator = basePath.path.iterator();
    while (pathIterator.hasNext() && basePathIterator.hasNext()) {
      Screen element = pathIterator.next();
      Screen basePathElement = basePathIterator.next();
      if (basePathElement.getName().equals(element.getName())) {
        context = basePath.contexts.get(element.getName());
        contexts.put(element.getName(), context);
      } else {
        context = factory.createContext(element, context);
        contexts.put(element.getName(), context);
        break;
      }
    }
    // Now we continue walking our new path, creating contexts as we go.
    while (pathIterator.hasNext()) {
      Screen element = pathIterator.next();
      context = factory.createContext(element, context);
      contexts.put(element.getName(), context);
    }
    // Finally, we can construct our new PathContext
    return new PathContext(context, path, contexts);
  }

  /** Finds the tail of this path which is not in the given path, and destroys it. */
  public void destroyNotIn(PathContext path, ScreenContextFactory factory) {
    Iterator<Screen> aPath = this.path.iterator();
    Iterator<Screen> bPath = path.path.iterator();
    while (aPath.hasNext() && bPath.hasNext()) {
      String aScreen = aPath.next().getName();
      String bScreen = bPath.next().getName();
      if (!aScreen.equals(bScreen)) {
        factory.destroyContext(contexts.get(aScreen));
        break;
      }
    }
  }

  @SuppressWarnings("ResourceType")
  public static PathContext get(Context context) {
    return checkNotNull((PathContext) context.getSystemService(SERVICE_NAME),
        "Expected to find a PathContext but did not.");
  }

  @Override public Object getSystemService(String name) {
    if (SERVICE_NAME.equals(name)) {
      return this;
    }
    return super.getSystemService(name);
  }
}

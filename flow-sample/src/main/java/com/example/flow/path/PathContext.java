/*
 * Copyright 2014 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.flow.path;

import android.content.Context;
import android.content.ContextWrapper;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.flow.util.Preconditions.checkArgument;
import static com.example.flow.util.Preconditions.checkNotNull;

public final class PathContext extends ContextWrapper {
  private static final String SERVICE_NAME = "PATH_CONTEXT";
  private static final Map<String, Context> EMPTY_CONTEXT_MAP = Collections.emptyMap();
  private final Path path;
  private final Map<String, Context> contexts;

  PathContext(Context baseContext, Path path, Map<String, Context> contexts) {
    super(baseContext);
    checkArgument(baseContext != null, "Leaf context may not be null.");
    checkArgument(path.elements().size() == contexts.size() - 1, "Path and context map are not the same size");
    if (!path.isRoot()) {
      Path leafPath = path.elements().get(path.elements().size() - 1);
      checkArgument(baseContext == contexts.get(leafPath.getName()),
          "For a non-root Path, baseContext must be Path leaf's context.");
    }
    this.path = path;
    this.contexts = contexts;
  }

  public static PathContext root(Context baseContext) {
    return new PathContext(baseContext, Path.ROOT, EMPTY_CONTEXT_MAP);
  }

  public static PathContext create(PathContext preserve, Path path,
      PathContextFactory factory) {
    if (path == Path.ROOT) throw new IllegalArgumentException("Path is empty.");
    List<Path> elements = path.elements();
    Map<String, Context> contexts = new LinkedHashMap<>();
    // We walk down the elements, reusing existing contexts for the elements we encounter.  As soon
    // as we encounter an element that doesn't already have a context, we stop.
    // Note: we will always have at least one shared element, the root.
    Context baseContext = null;
    Iterator<Path> pathIterator = elements.iterator();
    Iterator<Path> basePathIterator = preserve.path.elements().iterator();
    while (pathIterator.hasNext() && basePathIterator.hasNext()) {
      Path element = pathIterator.next();
      Path basePathElement = basePathIterator.next();
      if (basePathElement.getName().equals(element.getName())) {
        baseContext = preserve.contexts.get(element.getName());
        contexts.put(element.getName(), baseContext);
      } else {
        baseContext = factory.setUpContext(element, baseContext);
        contexts.put(element.getName(), baseContext);
        break;
      }
    }
    // Now we continue walking our new path, creating contexts as we go.
    while (pathIterator.hasNext()) {
      Path element = pathIterator.next();
      baseContext = factory.setUpContext(element, baseContext);
      contexts.put(element.getName(), baseContext);
    }
    // Finally, we can construct our new PathContext
    return new PathContext(baseContext, path, contexts);
  }

  /** Finds the tail of this path which is not in the given path, and destroys it. */
  public void destroyNotIn(PathContext path, PathContextFactory factory) {
    Iterator<Path> aPath = this.path.elements().iterator();
    Iterator<Path> bPath = path.path.elements().iterator();
    while (aPath.hasNext() && bPath.hasNext()) {
      String aScreen = aPath.next().getName();
      String bScreen = bPath.next().getName();
      if (!aScreen.equals(bScreen)) {
        factory.tearDownContext(contexts.get(aScreen));
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

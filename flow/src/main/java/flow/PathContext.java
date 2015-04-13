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

package flow;

import android.content.Context;
import android.content.ContextWrapper;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** @deprecated Use flow.path.PathContext from the flow-path module. */
@Deprecated
public final class PathContext extends ContextWrapper {
  private static final String SERVICE_NAME = "PATH_CONTEXT";
  private final Path path;
  private final Map<Path, Context> contexts;

  PathContext(Context baseContext, Path path, Map<Path, Context> contexts) {
    super(baseContext);
    Preconditions.checkArgument(baseContext != null, "Leaf context may not be null.");
    Preconditions.checkArgument(path.elements().size() == contexts.size(),
        "Path and context map are not the same size, path has %d elements and there are %d contexts",
        path.elements().size(), contexts.size());
    if (!path.isRoot()) {
      Path leafPath = path.elements().get(path.elements().size() - 1);
      Preconditions.checkArgument(baseContext == contexts.get(leafPath),
          "For a non-root Path, baseContext must be Path leaf's context.");
    }
    this.path = path;
    this.contexts = contexts;
  }

  public static PathContext root(Context baseContext) {
    return new PathContext(baseContext, Path.ROOT,
        Collections.singletonMap(Path.ROOT, baseContext));
  }

  public static PathContext create(PathContext preserve, Path path, PathContextFactory factory) {
    if (path == Path.ROOT) throw new IllegalArgumentException("Path is empty.");
    List<Path> elements = path.elements();
    Map<Path, Context> contexts = new LinkedHashMap<>();
    // We walk down the elements, reusing existing contexts for the elements we encounter.  As soon
    // as we encounter an element that doesn't already have a context, we stop.
    // Note: we will always have at least one shared element, the root.
    Context baseContext = null;
    Iterator<Path> pathIterator = elements.iterator();
    Iterator<Path> basePathIterator = preserve.path.elements().iterator();
    while (pathIterator.hasNext() && basePathIterator.hasNext()) {
      Path element = pathIterator.next();
      Path basePathElement = basePathIterator.next();
      if (basePathElement.equals(element)) {
        baseContext = preserve.contexts.get(element);
        contexts.put(element, baseContext);
      } else {
        baseContext = factory.setUpContext(element, baseContext);
        contexts.put(element, baseContext);
        break;
      }
    }
    // Now we continue walking our new path, creating contexts as we go.
    while (pathIterator.hasNext()) {
      Path element = pathIterator.next();
      baseContext = factory.setUpContext(element, baseContext);
      contexts.put(element, baseContext);
    }
    // Finally, we can construct our new PathContext
    return new PathContext(baseContext, path, contexts);
  }

  /** Finds the tail of this path which is not in the given path, and destroys it. */
  public void destroyNotIn(PathContext path, PathContextFactory factory) {
    Iterator<Path> aElements = this.path.elements().iterator();
    Iterator<Path> bElements = path.path.elements().iterator();
    while (aElements.hasNext() && bElements.hasNext()) {
      Path aElement = aElements.next();
      Path bElement = bElements.next();
      if (!aElement.equals(bElement)) {
        factory.tearDownContext(contexts.get(aElement));
        break;
      }
    }
  }

  @SuppressWarnings("ResourceType")
  public static PathContext get(Context context) {
    return Preconditions.checkNotNull((PathContext) context.getSystemService(SERVICE_NAME),
        "Expected to find a PathContext but did not.");
  }

  @Override public Object getSystemService(String name) {
    if (SERVICE_NAME.equals(name)) {
      return this;
    }
    return super.getSystemService(name);
  }
}

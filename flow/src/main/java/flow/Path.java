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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an absolute path in the logical information tree of an app, from the root of the tree
 * to a leaf, which is typically associated with app "screen" or "state". Each element in a path
 * typically represents a scope, narrowing and adding information from root to leaf.
 */
// TODO(#127): Should be an interface! Kotlin data classes in particular will need it to be.
public abstract class Path {
  static final Path ROOT = new Path() {
  };
  private transient List<Path> elements;

  protected void build(@SuppressWarnings("UnusedParameters") Builder builder) {
  }

  final List<Path> elements() {
    if (elements == null) {
      Builder builder = new Builder();
      build(builder);
      // For convenience, we don't require leaf classes to override build().
      if (builder.isNotTail(this)) {
        builder.append(this);
      }
      elements = builder.elements;
    }
    return elements;
  }

  final boolean isRoot() {
    return this == ROOT;
  }

  public static final class Builder {
    private final List<Path> elements = new ArrayList<>();

    Builder() {
      elements.add(ROOT);
    }

    public void append(Path path) {
      elements.add(path);
    }

    private boolean isNotTail(Path path) {
      return !path.equals(elements.get(elements.size() - 1));
    }
  }
}

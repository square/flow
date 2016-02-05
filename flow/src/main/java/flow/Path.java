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
 * Convenience implementation of {@link TreeKey}.
 *
 * Subclasses can override {@link #build(Builder)} to easily construct a key list based on class
 * hierarchy.
 */
public abstract class Path implements TreeKey {
  private transient List<Object> elements;

  protected void build(@SuppressWarnings("UnusedParameters") Builder builder) {
  }

  @Override public List<Object> getKeyPath() {
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

  public static final class Builder {
    private final List<Object> elements = new ArrayList<>();

    public void append(Object key) {
      elements.add(key);
    }

    private boolean isNotTail(TreeKey treeKey) {
      return !treeKey.equals(elements.get(elements.size() - 1));
    }
  }
}

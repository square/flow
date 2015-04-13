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
import android.view.LayoutInflater;
import java.util.ArrayList;
import java.util.List;

/** @deprecated Use flow.path.Path from the flow-path module. */
@Deprecated
public abstract class Path {
  static final Path ROOT = new Path() {
  };
  private transient List<Path> elements;

  public static PathContextFactory contextFactory() {
    return new ContextFactory();
  }

  public static PathContextFactory contextFactory(PathContextFactory delegate) {
    return new ContextFactory(delegate);
  }

  public static <T extends Path> T get(Context context) {
    LocalPathWrapper wrapper = LocalPathWrapper.get(context);
    if (wrapper == null) {
      throw new IllegalArgumentException("Supplied context has no Path");
    }
    // If this blows up, it's on the caller.  We hide the cast as a convenience.
    //noinspection unchecked
    return (T) wrapper.localScreen;
  }

  protected void build(Builder builder) {
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

  private static final class LocalPathWrapper extends ContextWrapper {
    static final String LOCAL_WRAPPER_SERVICE = "flow_local_screen_context_wrapper";
    private LayoutInflater inflater;

    static LocalPathWrapper get(Context context) {
      //noinspection ResourceType
      return (LocalPathWrapper) context.getSystemService(LOCAL_WRAPPER_SERVICE);
    }

    final Object localScreen;

    LocalPathWrapper(Context base, Object localScreen) {
      super(base);
      this.localScreen = localScreen;
    }

    @Override public Object getSystemService(String name) {
      if (LOCAL_WRAPPER_SERVICE.equals(name)) {
        return this;
      }
      if (LAYOUT_INFLATER_SERVICE.equals(name)) {
        if (inflater == null) {
          inflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
        }
        return inflater;
      }
      return super.getSystemService(name);
    }
  }

  private static final class ContextFactory implements PathContextFactory {
    // May be null.
    private final PathContextFactory delegate;

    public ContextFactory() {
      delegate = null;
    }

    public ContextFactory(PathContextFactory delegate) {
      this.delegate = delegate;
    }

    @Override public Context setUpContext(Path path, Context parentContext) {
      if (delegate != null) {
        parentContext = delegate.setUpContext(path, parentContext);
      }
      return new LocalPathWrapper(parentContext, path);
    }

    @Override public void tearDownContext(Context context) {
      if (delegate != null) {
        delegate.tearDownContext(context);
      }
    }
  }
}

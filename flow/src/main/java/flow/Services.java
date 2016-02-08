/*
 * Copyright 2016 Square Inc.
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

import android.support.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static flow.Preconditions.checkNotNull;

public class Services {
  static final Services ROOT_SERVICES =
      new Services(Flow.ROOT_KEY, null, Collections.<String, Object>emptyMap());

  public static final class Binder extends Services {
    private final Map<String, Object> services = new LinkedHashMap<>();
    private final Services base;

    private Binder(Services base, Object key) {
      super(key, base, Collections.<String, Object>emptyMap());
      checkNotNull(base, "only root Services should have a null base");
      this.base = base;
    }

    public Binder bind(String serviceName, Object service) {
      services.put(serviceName, service);
      return this;
    }

    Services build() {
      return new Services(getKey(), base, services);
    }
  }

  private final Object key;
  @Nullable private final Services delegate;
  private final Map<String, Object> localServices = new LinkedHashMap<>();

  private Services(Object key, @Nullable Services delegate, Map<String, Object> localServices) {
    this.delegate = delegate;
    this.key = key;
    this.localServices.putAll(localServices);
  }

  public <T> T getService(String name) {
    if (localServices.containsKey(name)) {
      @SuppressWarnings("unchecked") //
      final T service = (T) localServices.get(name);
      return service;
    }
    if (delegate != null) return delegate.getService(name);
    return null;
  }

  public <T> T getKey() {
    //noinspection unchecked
    return (T) this.key;
  }

  Binder extend(Object key) {
    return new Binder(this, key);
  }
}

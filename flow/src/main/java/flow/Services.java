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

public final class Services {
  static final Services ROOT = new Services(null, Collections.<String, Object>emptyMap());
  static final String KEY_SERVICE = "flow_local_key_service";

  public static final class Binder {
    private final Map<String, Object> services = new LinkedHashMap<>();
    private final Services base;

    private Binder(Services base) {
      checkNotNull(base, "only Services.ROOT should have a null base");
      this.base = base;
    }

    public <T> T getKey() {
      @SuppressWarnings("unchecked") T key = (T) services.get(KEY_SERVICE);
      return key;
    }

    public Binder bind(String serviceName, Object service) {
      services.put(serviceName, service);
      return this;
    }

    Services build() {
      return new Services(base, services);
    }
  }

  @Nullable private final Services delegate;
  private final Map<String, Object> localServices = new LinkedHashMap<>();

  private Services(@Nullable Services delegate, Map<String, Object> localServices) {
    this.delegate = delegate;
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
    return getService(Services.KEY_SERVICE);
  }

  Binder extend() {
    return new Binder(this);
  }
}

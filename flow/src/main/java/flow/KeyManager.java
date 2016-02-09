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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class KeyManager {
  static final Object ROOT_KEY = new Object() {
    @Override public String toString() {
      return KeyManager.class.getSimpleName() + ".ROOT";
    }
  };
  private final Map<Object, CountedServices> nodes = new LinkedHashMap<>();
  private final List<ServicesFactory> servicesFactories = new ArrayList<>();

  KeyManager(List<ServicesFactory> servicesFactories) {
    this.servicesFactories.addAll(servicesFactories);
    nodes.put(ROOT_KEY, new CountedServices(Services.ROOT_SERVICES));
  }

  Services findServices(Object key) {
    final CountedServices counted = nodes.get(key);
    if (counted == null) {
      throw new IllegalStateException("No services currently exists for key " + key);
    }
    return counted.services;
  }

  void setUp(Object key) {
    Services parent = nodes.get(ROOT_KEY).services;
    if (key instanceof MultiKey) {
      for (Object part : ((MultiKey) key).getKeys()) {
        setUp(part);
      }
      ensureNode(parent, key).uses++;
    } else if (key instanceof TreeKey) {
      TreeKey treeKey = (TreeKey) key;
      final Object parentKey = treeKey.getParentKey();
      setUp(parentKey);
      parent = nodes.get(parentKey).services;
      ensureNode(parent, key).uses++;
    } else {
      ensureNode(parent, key).uses++;
    }
  }

  void tearDown(Object key) {
    if (key instanceof MultiKey) {
      decrementAndMaybeRemoveKey(key);
      final List<Object> parts = ((MultiKey) key).getKeys();
      final int count = parts.size();
      for (int i = count - 1; i >= 0; i--) {
        tearDown(parts.get(i));
      }
    } else if (key instanceof TreeKey) {
      decrementAndMaybeRemoveKey(key);
      TreeKey treeKey = (TreeKey) key;
      tearDown(treeKey.getParentKey());
    } else {
      decrementAndMaybeRemoveKey(key);
    }
  }

  @NonNull private CountedServices ensureNode(@Nullable Services parent, Object key) {
    CountedServices node = nodes.get(key);
    if (node == null) {
      // Bind the local key as a service.
      @SuppressWarnings("ConstantConditions") //
      Services.Binder binder = parent.extend(key);
      // Add any services from the factories
      int count = servicesFactories.size();
      for (int i = 0; i < count; i++) {
        servicesFactories.get(i).bindServices(binder);
      }
      node = new CountedServices(binder.build());
      nodes.put(key, node);
    }
    return node;
  }

  private boolean decrementAndMaybeRemoveKey(Object key) {
    CountedServices node = nodes.get(key);
    node.uses--;
    if (key != ROOT_KEY && node.uses == 0) {
      int count = servicesFactories.size();
      for (int i = count - 1; i >= 0; i--) {
        servicesFactories.get(i).tearDownServices(node.services);
      }
      nodes.remove(key);
      return true;
    }
    if (node.uses < 0) {
      throw new IllegalStateException("Over-decremented uses of key " + key);
    }
    return false;
  }

  private static final class CountedServices {
    final Services services;
    /** Includes uses as a leaf and as a direct parent. */
    int uses = 0;

    private CountedServices(Services services) {
      this.services = services;
    }
  }
}

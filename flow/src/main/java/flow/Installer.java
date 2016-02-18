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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class Installer {

  private final Context baseContext;
  private final Activity activity;
  private final List<ServicesFactory> contextFactories = new ArrayList<>();
  private KeyParceler parceler;
  private Object defaultKey;
  private Dispatcher dispatcher;

  public Installer(Context baseContext, Activity activity) {
    this.baseContext = baseContext;
    this.activity = activity;
  }

  public Installer keyParceler(@Nullable KeyParceler parceler) {
    this.parceler = parceler;
    return this;
  }

  public Installer dispatcher(@Nullable Dispatcher dispatcher) {
    this.dispatcher = dispatcher;
    return this;
  }

  public Installer defaultKey(@Nullable Object defaultKey) {
    this.defaultKey = defaultKey;
    return this;
  }

  /**
   * Applies a factory when creating a Context associated with a given key.
   *
   * May be called multiple times. Factories are called in the order given during setup, and
   * in reverse order during teardown.
   */
  public Installer addServicesFactory(ServicesFactory factory) {
    contextFactories.add(factory);
    return this;
  }

  public Context install() {
    if (InternalLifecycleIntegration.find(activity) != null) {
      throw new IllegalStateException("Flow is already installed in this Activity.");
    }
    Dispatcher dispatcher = this.dispatcher;
    if (dispatcher == null) {
      dispatcher = KeyDispatcher.configure(activity, new DefaultKeyChanger(activity)) //
          .build();
    }
    final Object defState = defaultKey == null ? "Hello, World!" : defaultKey;

    final History defaultHistory = History.single(defState);
    final Application app = (Application) baseContext.getApplicationContext();
    final KeyManager keyManager = new KeyManager(contextFactories);
    InternalLifecycleIntegration.install(app, activity, parceler, defaultHistory, dispatcher,
        keyManager);
    return new InternalContextWrapper(baseContext, activity);
  }
}

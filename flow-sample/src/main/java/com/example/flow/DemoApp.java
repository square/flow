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

package com.example.flow;

import android.app.Application;
import com.example.flow.util.FlowBundler;
import com.google.gson.Gson;
import dagger.ObjectGraph;
import flow.Backstack;
import javax.annotation.Nullable;

public class DemoApp extends Application {
  private final FlowBundler flowBundler = new FlowBundler(new GsonParceler(new Gson())) {
    @Override protected Backstack getColdStartBackstack(@Nullable Backstack restoredBackstack) {
      return restoredBackstack == null ? Backstack.single(new Paths.ConversationList())
          : restoredBackstack;
    }
  };
  private ObjectGraph globalGraph;

  @Override public void onCreate() {
    super.onCreate();

    globalGraph = ObjectGraph.create(new DaggerConfig());
  }

  public FlowBundler getFlowBundler() {
    return flowBundler;
  }

  public ObjectGraph getGlobalGraph() {
    return globalGraph;
  }
}

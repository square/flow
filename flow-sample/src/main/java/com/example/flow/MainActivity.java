/*
 * Copyright 2013 Square Inc.
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

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.example.flow.path.Path;
import com.example.flow.pathview.FramePathContainerView;
import com.example.flow.util.FlowBundler;
import com.google.gson.Gson;
import flow.Flow;
import flow.HasParent;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static flow.Flow.Traversal;
import static flow.Flow.TraversalCallback;

public class MainActivity extends Activity implements Flow.Dispatcher {
  /**
   * Persists the {@link Flow} in the bundle. Initialized with the home path,
   * {@link Paths.ConversationList}.
   */
  private final FlowBundler flowBundler =
      new FlowBundler(new Paths.ConversationList(), MainActivity.this,
          new GsonParcer<>(new Gson()));

  @InjectView(R.id.container) FramePathContainerView container;

  private Flow flow;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    flow = flowBundler.onCreate(savedInstanceState);

    final ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);

    setContentView(R.layout.activity_main);
    ButterKnife.inject(this);

    Flow.loadInitialScreen(this);
  }

  @Override public Object getSystemService(String name) {
    if (Flow.isFlowSystemService(name)) return flow;
    return super.getSystemService(name);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    flowBundler.onSaveInstanceState(outState);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem friendsMenu = menu.add("Friends")
        .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
          @Override public boolean onMenuItemClick(MenuItem menuItem) {
            Flow.get(MainActivity.this).goTo(new Paths.FriendList());
            return true;
          }
        });

    Object screen = Flow.get(this).getBackstack().current().getScreen();
    boolean hasUp = screen instanceof HasParent;
    friendsMenu.setVisible(!hasUp);

    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      return container.onUpPressed();
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onBackPressed() {
    if (!container.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
    Path path = (Path) traversal.destination.current().getScreen();
    container.showScreen(path, traversal.direction, callback);

    setTitle(path.getClass().getSimpleName());

    ActionBar actionBar = getActionBar();
    boolean hasUp = path instanceof HasParent;
    actionBar.setDisplayHomeAsUpEnabled(hasUp);
    actionBar.setHomeButtonEnabled(hasUp);

    invalidateOptionsMenu();
  }
}

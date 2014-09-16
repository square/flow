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
import com.example.flow.appflow.AppFlow;
import com.example.flow.appflow.FlowBundler;
import com.example.flow.appflow.Screen;
import com.example.flow.screenswitcher.FrameScreenSwitcherView;
import flow.Backstack;
import flow.Flow;
import flow.HasParent;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class MainActivity extends Activity implements Flow.Listener {
  private static final String BUNDLE_BACKSTACK = "backstack";

  private final FlowBundler flowBundler =
      new FlowBundler(MainActivity.this, new Screens.ConversationList());

  @InjectView(R.id.container) FrameScreenSwitcherView container;

  private AppFlow.FlowHolder flowHolder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    ButterKnife.inject(this);

    final ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);

    DemoApp app = (DemoApp) getApplication();

    flowHolder = flowBundler.onCreate(savedInstanceState);

    AppFlow.loadInitialScreen(this);
  }

  @Override public Object getSystemService(String name) {
    if (AppFlow.isFlowHolderSystemService(name)) return flowHolder;
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
            AppFlow.get(MainActivity.this).goTo(new Screens.FriendList());
            return true;
          }
        });

    Object screen = AppFlow.get(this).getBackstack().current().getScreen();
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

  @Override public void go(Backstack nextBackstack, Flow.Direction direction) {
    Screen screen = (Screen) nextBackstack.current().getScreen();
    container.showScreen(screen);

    setTitle(screen.getClass().getSimpleName());

    ActionBar actionBar = getActionBar();
    boolean hasUp = screen instanceof HasParent;
    actionBar.setDisplayHomeAsUpEnabled(hasUp);
    actionBar.setHomeButtonEnabled(hasUp);

    invalidateOptionsMenu();
  }
}

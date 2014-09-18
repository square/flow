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
import com.example.flow.appflow.AppFlow;
import com.example.flow.appflow.FlowBundler;
import com.example.flow.appflow.Screen;
import com.example.flow.screenswitcher.CanShowScreen;
import com.example.flow.screenswitcher.HandlesBack;
import com.example.flow.screenswitcher.HandlesUp;
import com.google.gson.Gson;
import flow.Backstack;
import flow.Flow;
import flow.HasParent;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class MainActivity extends Activity implements Flow.Listener {
  /**
   * Persists the {@link Flow} in the bundle. Initialized with the home screen,
   * {@link Screens.ConversationList}.
   */
  private final FlowBundler flowBundler =
      new FlowBundler(new Screens.ConversationList(), MainActivity.this,
          new GsonParcer<>(new Gson()));

  private CanShowScreen container;
  private HandlesBack containerAsBackTarget;
  private HandlesUp containerAsUpTarget;

  private AppFlow appFlow;

  /**
   * Pay attention to the {@link #setContentView} call here. It's creating a responsive layout
   * for us.
   * <p>
   * Notice that the app has two root_layout files. The main one, in {@code res/layout} is used by
   * mobile devices and by tablets in portrait orientation. It holds a generic {@link
   * com.example.flow.screenswitcher.FrameScreenSwitcherView}.
   * <p>
   * The interesting one, loaded by tablets in landscape mode, is {@code res/layout-sw600dp-land}.
   * It loads a {@link com.example.flow.view.TabletMasterDetailRoot}, with a master list on the
   * left and a detail view on the right.
   * <p>
   * But this master activity knows nothing about those two view types. It only requires that
   * the view loaded by {@code root_layout.xml} implements the {@link CanShowScreen} interface,
   * to render whatever is appropriate for the screens received from {@link Flow} via
   * {@link #go}.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appFlow = flowBundler.onCreate(savedInstanceState);

    final ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);

    setContentView(R.layout.root_layout);

    container = (CanShowScreen) findViewById(R.id.container);
    containerAsBackTarget = (HandlesBack) container;
    containerAsUpTarget = (HandlesUp) container;

    AppFlow.loadInitialScreen(this);
  }

  @Override public Object getSystemService(String name) {
    if (AppFlow.isAppFlowSystemService(name)) return appFlow;
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
      return containerAsUpTarget.onUpPressed();
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onBackPressed() {
    if (!containerAsBackTarget.onBackPressed()) {
      super.onBackPressed();
    }
  }

  /**
   * Called by {@link Flow} when it's time to show a new screen. Updates the action
   * bar and passes the screen to the {@link #container} we loaded from {@code root_layout.xml}
   * via {@link CanShowScreen#showScreen}.
   */
  @Override public void go(Backstack nextBackstack, Flow.Direction direction,
      Flow.Callback callback) {
    Screen screen = (Screen) nextBackstack.current().getScreen();
    container.showScreen(screen, direction, callback);

    setTitle(screen.getClass().getSimpleName());

    ActionBar actionBar = getActionBar();
    boolean hasUp = screen instanceof HasParent;
    actionBar.setDisplayHomeAsUpEnabled(hasUp);
    actionBar.setHomeButtonEnabled(hasUp);

    invalidateOptionsMenu();
  }
}

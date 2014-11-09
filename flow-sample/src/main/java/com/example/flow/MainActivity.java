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
import com.example.flow.pathview.HandlesBack;
import com.example.flow.pathview.HandlesUp;
import com.example.flow.util.FlowBundler;
import flow.Flow;
import flow.HasParent;
import flow.Path;
import flow.PathContainerView;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static flow.Flow.Traversal;
import static flow.Flow.TraversalCallback;

public class MainActivity extends Activity implements Flow.Dispatcher {
  private PathContainerView container;
  private HandlesBack containerAsBackTarget;
  private HandlesUp containerAsUpTarget;

  private Flow flow;

  /**
   * Pay attention to the {@link #setContentView} call here. It's creating a responsive layout
   * for us.
   * <p>
   * Notice that the app has two root_layout files. The main one, in {@code res/layout} is used by
   * mobile devices and by tablets in portrait orientation. It holds a generic {@link
   * com.example.flow.pathview.FramePathContainerView}.
   * <p>
   * The interesting one, loaded by tablets in landscape mode, is {@code res/layout-sw600dp-land}.
   * It loads a {@link com.example.flow.view.TabletMasterDetailRoot}, with a master list on the
   * left and a detail view on the right.
   * <p>
   * But this master activity knows nothing about those two view types. It only requires that
   * the view loaded by {@code root_layout.xml} implements the {@link PathContainerView} interface,
   * to render whatever is appropriate for the screens received from {@link Flow} via
   * {@link #dispatch}.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    flow = getFlowBundler().onCreate(savedInstanceState);

    final ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);

    setContentView(R.layout.root_layout);

    container = (PathContainerView) findViewById(R.id.container);
    containerAsBackTarget = (HandlesBack) container;
    containerAsUpTarget = (HandlesUp) container;
  }

  @Override protected void onResume() {
    super.onResume();
    flow.setDispatcher(this);
  }

  @Override protected void onPause() {
    flow.removeDispatcher(this);
    super.onPause();
  }

  @Override public Object getSystemService(String name) {
    if (Flow.isFlowSystemService(name)) return flow;
    return super.getSystemService(name);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    getFlowBundler().onSaveInstanceState(outState);
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

    Object screen = Flow.get(this).getBackstack().current();
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

  @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
    Path path = traversal.destination.current();
    container.executeTraversal(traversal, callback);

    setTitle(path.getClass().getSimpleName());

    ActionBar actionBar = getActionBar();
    boolean hasUp = path instanceof HasParent;
    actionBar.setDisplayHomeAsUpEnabled(hasUp);
    actionBar.setHomeButtonEnabled(hasUp);

    invalidateOptionsMenu();
  }

  private FlowBundler getFlowBundler() {
    return ((DemoApp) getApplication()).getFlowBundler();
  }
}

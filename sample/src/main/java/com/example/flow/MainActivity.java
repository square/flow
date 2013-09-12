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
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import butterknife.InjectView;
import butterknife.Views;
import com.example.flow.model.Conversation;
import com.example.flow.model.User;
import com.example.flow.view.ContainerView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.flow.Backstack;
import com.squareup.flow.Flow;
import com.squareup.flow.HasParent;
import com.squareup.flow.Parcer;
import com.squareup.flow.Screen;
import com.squareup.flow.Screens;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class MainActivity extends Activity implements Flow.Listener {
  private static final String BUNDLE_BACKSTACK = "backstack";

  @InjectView(R.id.container) ContainerView containerView;
  @Inject Parcer<Screen> parcer;

  private Flow flow;
  private ObjectGraph activityGraph;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    Views.inject(this);

    final ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);

    activityGraph = ObjectGraph.create(new ActivityModule());
    activityGraph.inject(this);

    flow = new Flow(getInitialBackstack(savedInstanceState), this);

    go(flow.getBackstack(), Flow.Direction.FORWARD);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putParcelable(BUNDLE_BACKSTACK, flow.getBackstack().getParcelable(parcer));
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem friendsMenu = menu.add("Friends")
        .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
          @Override public boolean onMenuItemClick(MenuItem menuItem) {
            flow.goTo(new App.FriendList());
            return true;
          }
        });

    Screen screen = flow.getBackstack().current().getScreen();
    boolean hasUp = screen instanceof HasParent;
    friendsMenu.setVisible(!hasUp);

    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      return flow.goUp();
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override public void onBackPressed() {
    if (!flow.goBack()) {
      finish();
    }
  }

  @Override public void go(Backstack backstack, Flow.Direction direction) {
    Screen screen = backstack.current().getScreen();
    containerView.displayView(getView(screen), direction);

    setTitle(screen.getClass().getSimpleName());

    ActionBar actionBar = getActionBar();
    boolean hasUp = screen instanceof HasParent;
    actionBar.setDisplayHomeAsUpEnabled(hasUp);
    actionBar.setHomeButtonEnabled(hasUp);

    invalidateOptionsMenu();
  }

  private Backstack getInitialBackstack(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      return Backstack.from(savedInstanceState.getParcelable(BUNDLE_BACKSTACK), parcer);
    } else {
      return Backstack.single(new App.ConversationList());
    }
  }

  private View getView(Screen screen) {
    ObjectGraph graph = activityGraph.plus(screen);
    Context scopedContext = new ScopedContext(this, graph);
    return Screens.createView(scopedContext, screen);
  }

  @Module(injects = MainActivity.class, library = true)
  class ActivityModule {
    @Provides @App Flow provideAppFlow() {
      return flow;
    }

    @Provides List<Conversation> provideConversations() {
      return SampleData.CONVERSATIONS;
    }

    @Provides List<User> provideFriends() {
      return SampleData.FRIENDS;
    }

    @Provides @Singleton Gson provideGson() {
      return new GsonBuilder().create();
    }

    @Provides @Singleton Parcer<Screen> provideParcer(Gson gson) {
      return new GsonParcer<Screen>(gson);
    }
  }
}

package com.example.flow;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import butterknife.InjectView;
import butterknife.Views;
import com.example.flow.model.Conversation;
import com.example.flow.model.User;
import com.squareup.flow.Backstack;
import com.squareup.flow.Flow;
import com.squareup.flow.Screen;
import com.squareup.flow.Screens;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import java.util.List;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class MainActivity extends Activity implements Flow.Listener {
  @InjectView(R.id.container) FrameLayout containerView;

  private MenuItem friendsMenu;

  private Flow flow;
  private ObjectGraph activityGraph;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    Views.inject(this);

    final ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);

    flow = new Flow(Backstack.single(new App.ConversationList()), this);
    activityGraph = ObjectGraph.create(new ActivityModule());

    invalidateOptionsMenu();
    go(flow.getBackstack(), Flow.Direction.FORWARD);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    friendsMenu = menu.add("Friends")
        .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
          @Override public boolean onMenuItemClick(MenuItem menuItem) {
            flow.goTo(new App.FriendList());
            return true;
          }
        });
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
    displayView(getView(screen));

    setTitle(screen.getClass().getSimpleName());

    ActionBar actionBar = getActionBar();
    boolean hasUp = backstack.current().getScreen() instanceof Screen.HasParent<?>;
    actionBar.setDisplayHomeAsUpEnabled(hasUp);
    actionBar.setHomeButtonEnabled(hasUp);

    if (friendsMenu == null) return;
    friendsMenu.setVisible(!hasUp);
  }

  private View getView(Screen screen) {
    ObjectGraph graph = activityGraph.plus(screen);
    Context scopedContext = new ScopedContext(this, graph);
    return Screens.createView(scopedContext, screen);
  }

  private void displayView(View view) {
    containerView.removeAllViews();
    containerView.addView(view);
  }

  @Module(library = true) class ActivityModule {
    @Provides @App Flow provideAppFlow() {
      return flow;
    }

    @Provides List<Conversation> provideConversations() {
      return SampleData.CONVERSATIONS;
    }

    @Provides List<User> provideFriends() {
      return SampleData.FRIENDS;
    }
  }
}

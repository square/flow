package com.example.flow;

import com.example.flow.model.Conversation;
import com.example.flow.model.User;
import com.example.flow.view.ConversationListView;
import com.example.flow.view.ConversationView;
import com.example.flow.view.FriendListView;
import com.example.flow.view.FriendView;
import com.example.flow.view.MessageView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dagger.Module;
import dagger.Provides;
import flow.Parcer;
import java.util.List;
import javax.inject.Singleton;

@Module(
    injects = {
        ConversationView.class, //
        ConversationListView.class, //
        FriendView.class, //
        FriendListView.class, //
        MessageView.class,
    },
    library = true)
public class DaggerConfig {
  @Provides List<Conversation> provideConversations() {
    return SampleData.CONVERSATIONS;
  }

  @Provides List<User> provideFriends() {
    return SampleData.FRIENDS;
  }

  @Provides @Singleton Gson provideGson() {
    return new GsonBuilder().create();
  }

  @Provides @Singleton Parcer<Object> provideParcer(Gson gson) {
    return new GsonParcer<>(gson);
  }
}

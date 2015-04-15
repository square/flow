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
import flow.StateParceler;
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

  @Provides @Singleton StateParceler provideParcer(Gson gson) {
    return new GsonParceler(gson);
  }
}

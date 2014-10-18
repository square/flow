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

import com.example.flow.path.Path;
import flow.HasParent;
import flow.Layout;

public final class Paths {
  @Layout(R.layout.conversation_list_view) //
  public static class ConversationList extends Path {
  }

  @Layout(R.layout.conversation_view) //
  public static class Conversation extends Path implements HasParent<ConversationList> {
    public final int conversationIndex;

    public Conversation(int conversationIndex) {
      this.conversationIndex = conversationIndex;
    }

    @Override public ConversationList getParent() {
      return new ConversationList();
    }
  }

  @Layout(R.layout.message_view) //
  public static class Message extends Path implements HasParent<Conversation> {
    public final int conversationIndex;
    public final int messageId;

    public Message(int conversationIndex, int messageId) {
      this.conversationIndex = conversationIndex;
      this.messageId = messageId;
    }

    @Override public Conversation getParent() {
      return new Conversation(conversationIndex);
    }
  }

  @Layout(R.layout.friend_list_view) //
  public static class FriendList extends Path implements HasParent<ConversationList> {
    @Override public ConversationList getParent() {
      return new ConversationList();
    }
  }

  @Layout(R.layout.friend_view) //
  public static class Friend extends Path implements HasParent<FriendList> {
    public final int index;

    public Friend(int index) {
      this.index = index;
    }

    @Override public FriendList getParent() {
      return new FriendList();
    }
  }

  private Paths() {
  }
}

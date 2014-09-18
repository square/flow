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

import com.example.flow.appflow.Screen;
import com.example.flow.view.TabletMasterDetailRoot;
import flow.HasParent;
import flow.Layout;

public final class Screens {
  @Layout(R.layout.no_details)
  public static class NoDetails extends Screen {
  }

  /**
   * Identifies screens in a master / detail relationship. Both master and detail screens
   * extend this class.
   * <p>
   * Not a lot of thought has been put into making a decent master / detail modeling here. Rather
   * this is an excuse to show off using Flow to build a responsive layout. See {@link
   * TabletMasterDetailRoot}.
   */
  public static abstract class MasterDetailScreen extends Screen {
    /**
     * Returns the screen that shows the master list for this type of screen.
     * If this screen is the master, returns self.
     * <p>
     * For example, the {@link Conversation} and {@link Message} screens are both
     * "under" the master {@link ConversationList} screen. All three of these
     * screens return a {@link Conversation} from this method.
     */
    public abstract MasterDetailScreen getMaster();

    final public boolean isMaster() {
      return equals(getMaster());
    }
  }

  public abstract static class ConversationScreen extends MasterDetailScreen {
    public final int conversationIndex;

    protected ConversationScreen(int conversationIndex) {
      this.conversationIndex = conversationIndex;
    }

    @Override public MasterDetailScreen getMaster() {
      return new ConversationList();
    }
  }

  @Layout(R.layout.conversation_list_view) //
  public static class ConversationList extends ConversationScreen {
    public ConversationList() {
      super(-1);
    }
  }

  @Layout(R.layout.conversation_view) //
  public static class Conversation extends ConversationScreen
      implements HasParent<ConversationList> {

    public Conversation(int conversationIndex) {
      super(conversationIndex);
    }

    @Override public ConversationList getParent() {
      return new ConversationList();
    }

    @Override public String getName() {
      return "Conversation{" +
          "conversationIndex=" + conversationIndex +
          '}';
    }
  }

  @Layout(R.layout.message_view) //
  public static class Message extends ConversationScreen implements HasParent<Conversation> {
    public final int messageId;

    public Message(int conversationIndex, int messageId) {
      super(conversationIndex);
      this.messageId = messageId;
    }

    @Override public Conversation getParent() {
      return new Conversation(conversationIndex);
    }

    @Override public String getName() {
      return "Message{" +
          "conversationIndex=" + conversationIndex +
          ", messageId=" + messageId +
          '}';
    }
  }

  public static class FriendScreen extends MasterDetailScreen {
    public final int index;

    public FriendScreen(int index) {
      this.index = index;
    }

    @Override public MasterDetailScreen getMaster() {
      return new FriendList();
    }
  }

  @Layout(R.layout.friend_list_view) //
  public static class FriendList extends FriendScreen implements HasParent<ConversationList> {
    public FriendList() {
      super(-1);
    }

    @Override public ConversationList getParent() {
      return new ConversationList();
    }
  }

  @Layout(R.layout.friend_view) //
  public static class Friend extends FriendScreen implements HasParent<FriendList> {
    public Friend(int index) {
      super(index);
    }

    @Override public FriendList getParent() {
      return new FriendList();
    }

    @Override public String getName() {
      return "Friend{" +
          "index=" + index +
          '}';
    }
  }

  private Screens() {
  }
}

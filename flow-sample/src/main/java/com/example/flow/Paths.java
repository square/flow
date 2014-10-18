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

import flow.HasParent;
import flow.Layout;
import flow.Path;

public final class Paths {

  @Layout(R.layout.no_details)
  public static class NoDetails extends Path {
  }

  /**
   * Identifies screens in a master / detail relationship. Both master and detail screens
   * extend this class.
   * <p>
   * Not a lot of thought has been put into making a decent master / detail modeling here. Rather
   * this is an excuse to show off using Flow to build a responsive layout. See {@link
   * com.example.flow.view.TabletMasterDetailRoot}.
   */
  public abstract static class MasterDetailPath extends Path {
    /**
     * Returns the screen that shows the master list for this type of screen.
     * If this screen is the master, returns self.
     * <p>
     * For example, the {@link Conversation} and {@link Message} screens are both
     * "under" the master {@link ConversationList} screen. All three of these
     * screens return a {@link Conversation} from this method.
     */
    public abstract MasterDetailPath getMaster();

    public final boolean isMaster() {
      return equals(getMaster());
    }
  }

  public abstract static class ConversationPath extends MasterDetailPath {
    public final int conversationIndex;

    protected ConversationPath(int conversationIndex) {
      this.conversationIndex = conversationIndex;
    }

    @Override public MasterDetailPath getMaster() {
      return new ConversationList();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ConversationPath that = (ConversationPath) o;

      return conversationIndex == that.conversationIndex;
    }

    @Override
    public int hashCode() {
      return conversationIndex;
    }
  }

  @Layout(R.layout.conversation_list_view) //
  public static class ConversationList extends ConversationPath {
    public ConversationList() {
      super(-1);
    }
  }

  @Layout(R.layout.conversation_view) //
  public static class Conversation extends ConversationPath implements HasParent {

    public Conversation(int conversationIndex) {
      super(conversationIndex);
    }

    @Override public ConversationList getParent() {
      return new ConversationList();
    }
  }

  @Layout(R.layout.message_view) //
  public static class Message extends ConversationPath implements HasParent {
    public final int messageId;

    public Message(int conversationIndex, int messageId) {
      super(conversationIndex);
      this.messageId = messageId;
    }

    @Override public Conversation getParent() {
      return new Conversation(conversationIndex);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      Message message = (Message) o;

      return messageId == message.messageId;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + messageId;
      return result;
    }
  }

  public abstract static class FriendPath extends MasterDetailPath {
    public final int index;

    public FriendPath(int index) {
      this.index = index;
    }

    @Override public MasterDetailPath getMaster() {
      return new FriendList();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      FriendPath that = (FriendPath) o;

      return index == that.index;
    }

    @Override
    public int hashCode() {
      return index;
    }
  }

  @Layout(R.layout.friend_list_view) //
  public static class FriendList extends FriendPath implements HasParent {
    public FriendList() {
      super(-1);
    }

    @Override public ConversationList getParent() {
      return new ConversationList();
    }
  }

  @Layout(R.layout.friend_view) //
  public static class Friend extends FriendPath implements HasParent {
    public Friend(int index) {
      super(index);
    }

    @Override public FriendList getParent() {
      return new FriendList();
    }
  }

  private Paths() {
  }
}

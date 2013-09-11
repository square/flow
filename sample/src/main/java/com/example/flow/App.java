package com.example.flow;

import android.os.Parcel;
import android.os.Parcelable;
import com.example.flow.model.User;
import com.example.flow.view.ConversationListView;
import com.example.flow.view.ConversationView;
import com.example.flow.view.FriendListView;
import com.example.flow.view.FriendView;
import com.example.flow.view.MessageView;
import com.squareup.flow.Screen;
import com.squareup.flow.annotation.Layout;
import com.squareup.flow.annotation.View;
import dagger.Module;
import dagger.Provides;

public @interface App {
  @View(ConversationListView.class) //
  @Module(injects = ConversationListView.class, addsTo = MainActivity.ActivityModule.class)
  public static class ConversationList implements Screen {
    @Override public int describeContents() {
      return 0;
    }

    @Override public void writeToParcel(Parcel parcel, int i) {
    }

    public static final Parcelable.Creator<ConversationList> CREATOR =
        Parcelables.emptyCreator(ConversationList.class);
  }

  @View(ConversationView.class) //
  @Module(injects = ConversationView.class, addsTo = MainActivity.ActivityModule.class)
  public static class Conversation implements Screen, Screen.HasParent<ConversationList> {
    public final int conversationIndex;

    public Conversation(int conversationIndex) {
      this.conversationIndex = conversationIndex;
    }

    @Provides com.example.flow.model.Conversation provideConversation() {
      return SampleData.CONVERSATIONS.get(conversationIndex);
    }

    @Override public ConversationList getParent() {
      return new ConversationList();
    }

    @Override public int describeContents() {
      return 0;
    }

    @Override public void writeToParcel(Parcel out, int flags) {
      out.writeInt(conversationIndex);
    }

    public static final Parcelable.Creator<Conversation> CREATOR =
        new Parcelable.Creator<Conversation>() {
          @Override public Conversation createFromParcel(Parcel in) {
            int index = in.readInt();
            return new Conversation(index);
          }

          @Override public Conversation[] newArray(int size) {
            return new Conversation[size];
          }
        };
  }

  @Layout(R.layout.message_view) //
  @Module(injects = MessageView.class, addsTo = MainActivity.ActivityModule.class)
  public static class Message implements Screen, Screen.HasParent<Conversation> {
    public final int conversationIndex;
    public final int messageId;

    public Message(int conversationIndex, int messageId) {
      this.conversationIndex = conversationIndex;
      this.messageId = messageId;
    }

    @Provides com.example.flow.model.Conversation.Item provideMessage() {
      return SampleData.CONVERSATIONS.get(conversationIndex).items.get(messageId);
    }

    @Override public Conversation getParent() {
      return new Conversation(conversationIndex);
    }

    @Override public int describeContents() {
      return 0;
    }

    @Override public void writeToParcel(Parcel out, int flags) {
      out.writeInt(conversationIndex);
      out.writeInt(messageId);
    }

    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
      @Override public Message createFromParcel(Parcel in) {
        int conversationIndex = in.readInt();
        int messageIndex = in.readInt();
        return new Message(conversationIndex, messageIndex);
      }

      @Override public Message[] newArray(int size) {
        return new Message[size];
      }
    };
  }

  @View(FriendListView.class) //
  @Module(injects = FriendListView.class, addsTo = MainActivity.ActivityModule.class)
  public static class FriendList implements Screen, Screen.HasParent<ConversationList> {
    @Override public ConversationList getParent() {
      return new ConversationList();
    }

    @Override public int describeContents() {
      return 0;
    }

    @Override public void writeToParcel(Parcel parcel, int i) {

    }

    public static final Parcelable.Creator<FriendList> CREATOR =
        Parcelables.emptyCreator(FriendList.class);
  }

  @View(FriendView.class) //
  @Module(injects = FriendView.class)
  public static class Friend implements Screen, Screen.HasParent<FriendList> {
    public final int index;

    public Friend(int index) {
      this.index = index;
    }

    @Provides User provideFriend() {
      return SampleData.FRIENDS.get(index);
    }

    @Override public FriendList getParent() {
      return new FriendList();
    }

    @Override public int describeContents() {
      return 0; // TODO
    }

    @Override public void writeToParcel(Parcel parcel, int i) {
      // TODO
    }
  }
}

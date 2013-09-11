package com.example.flow;

import com.example.flow.model.Conversation;
import com.example.flow.model.User;
import java.util.Arrays;
import java.util.List;

public class SampleData {
  public static final List<User> FRIENDS;
  public static final List<Conversation> CONVERSATIONS;

  static {
    User me = new User("Me");
    User alex = new User("Alex");
    User chris = new User("Chris");

    FRIENDS = Arrays.asList(alex, chris);
    CONVERSATIONS = Arrays.asList(
        new Conversation(Arrays.asList(alex, chris), Arrays.asList(
            new Conversation.Item(me, "What's up?"),
            new Conversation.Item(alex, "Not much."),
            new Conversation.Item(chris, "Wanna hang out?"),
            new Conversation.Item(me, "Sure."),
            new Conversation.Item(alex, "Let's do it.")
        )),
        new Conversation(Arrays.asList(chris), Arrays.asList(
            new Conversation.Item(me, "You there bro?")
        ))
    );
  }

  private SampleData() {}
}

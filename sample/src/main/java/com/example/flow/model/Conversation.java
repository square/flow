package com.example.flow.model;

import android.text.TextUtils;
import java.util.List;

public class Conversation {
  public final List<User> users;
  public final List<Item> items;

  public Conversation(List<User> users, List<Item> items) {
    this.users = users;
    this.items = items;
  }

  @Override public String toString() {
    return TextUtils.join(", ", users.toArray(new User[0]));
  }

  public static class Item {
    public final User from;
    public final String message;

    public Item(User from, String message) {
      this.from = from;
      this.message = message;
    }

    @Override public String toString() {
      return from + ": " + message;
    }
  }
}

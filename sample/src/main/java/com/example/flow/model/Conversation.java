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

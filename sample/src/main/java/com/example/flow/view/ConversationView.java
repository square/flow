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

package com.example.flow.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.example.flow.App;
import com.example.flow.SampleData;
import com.example.flow.Utils;
import com.example.flow.model.Conversation;
import com.squareup.flow.Flow;
import java.util.List;
import javax.inject.Inject;

public class ConversationView extends ListView {
  @Inject @App Flow flow;
  @Inject Conversation conversation;

  public ConversationView(Context context, AttributeSet attrs) {
    super(context, attrs);

    Utils.inject(context, this);
    setConversation(conversation);
  }

  private void setConversation(final Conversation conversation) {
    Adapter adapter = new Adapter(getContext(), conversation.items);

    setAdapter(adapter);
    setOnItemClickListener(new OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int conversationIndex = SampleData.CONVERSATIONS.indexOf(conversation);
        flow.goTo(new App.Message(conversationIndex, position));
      }
    });
  }

  private static class Adapter extends ArrayAdapter<Conversation.Item> {
    public Adapter(Context context, List<Conversation.Item> objects) {
      super(context, android.R.layout.simple_list_item_1, objects);
    }
  }
}

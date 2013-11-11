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
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Views;
import com.example.flow.App;
import com.example.flow.R;
import com.example.flow.SampleData;
import com.example.flow.Utils;
import com.example.flow.model.Conversation;
import flow.Flow;
import javax.inject.Inject;

public class MessageView extends LinearLayout {
  @Inject @App Flow flow;
  @Inject Conversation.Item message;

  @InjectView(R.id.user) TextView userView;
  @InjectView(R.id.message) TextView messageView;

  public MessageView(Context context, AttributeSet attrs) {
    super(context, attrs);

    setOrientation(VERTICAL);

    Utils.inject(context, this);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    Views.inject(this);

    userView.setText(String.valueOf(message.from));
    messageView.setText(String.valueOf(message.message));
  }

  @OnClick(R.id.user) void userClicked() {
    int position = SampleData.FRIENDS.indexOf(message.from);
    if (position != -1) {
      flow.goTo(new App.Friend(position));
    }
  }
}

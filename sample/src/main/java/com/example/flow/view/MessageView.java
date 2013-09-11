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
import com.squareup.flow.Flow;
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

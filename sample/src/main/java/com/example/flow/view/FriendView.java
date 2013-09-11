package com.example.flow.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import com.example.flow.Utils;
import com.example.flow.model.User;
import javax.inject.Inject;

public class FriendView extends TextView {
  @Inject User friend;

  public FriendView(Context context, AttributeSet attrs) {
    super(context, attrs);

    Utils.inject(context, this);
    setText("Name: " + friend.name);
  }
}

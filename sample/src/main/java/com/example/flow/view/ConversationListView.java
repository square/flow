package com.example.flow.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.example.flow.App;
import com.example.flow.Utils;
import com.example.flow.model.Conversation;
import com.squareup.flow.Flow;
import java.util.List;
import javax.inject.Inject;

public class ConversationListView extends ListView {
  @Inject @App Flow flow;
  @Inject List<Conversation> conversations;

  public ConversationListView(Context context, AttributeSet attrs) {
    super(context, attrs);

    Utils.inject(context, this);

    Adapter adapter = new Adapter(getContext(), conversations);

    setAdapter(adapter);
    setOnItemClickListener(new OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        flow.goTo(new App.Conversation(position));
      }
    });
  }

  private static class Adapter extends ArrayAdapter<Conversation> {
    public Adapter(Context context, List<Conversation> objects) {
      super(context, android.R.layout.simple_list_item_1, objects);
    }
  }
}

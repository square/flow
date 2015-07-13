package flow;

import android.app.Activity;
import android.view.Gravity;
import android.widget.TextView;

final class DefaultDispatcher implements Flow.Dispatcher {
  private final Activity activity;
  private TextView textView;

  DefaultDispatcher(Activity activity) {
    this.activity = activity;
  }

  @Override public void dispatch(Flow.Traversal traversal, Flow.TraversalCallback callback) {
    if (textView == null) {
      textView = new TextView(activity);
      textView.setGravity(Gravity.CENTER);
    }
    textView.setText(traversal.destination.top().toString());
    activity.setContentView(textView);
    callback.onTraversalCompleted();
  }
}

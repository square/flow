package flow.sample.basic;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import flow.Flow;

final class BasicDispatcher implements Flow.Dispatcher {

  private final Activity activity;

  BasicDispatcher(Activity activity) {
    this.activity = activity;
  }

  @Override public void dispatch(Flow.Traversal traversal, Flow.TraversalCallback callback) {
    Log.d("BasicDispatcher", "dispatching " + traversal);
    Object dest = traversal.destination.top();

    ViewGroup frame = (ViewGroup) activity.findViewById(R.id.basic_activity_frame);

    if (traversal.origin != null) {
      if (frame.getChildCount() > 0) {
        traversal.origin.topViewState().save(frame.getChildAt(0));
        frame.removeAllViews();
      }
    }

    @LayoutRes final int layout;
    if (dest instanceof HelloScreen) {
      layout = R.layout.hello_screen;
    } else if (dest instanceof WelcomeScreen) {
      layout = R.layout.welcome_screen;
    } else {
      throw new AssertionError("Unrecognized screen " + dest);
    }

    Context screenContext = new Screens.ContextWrapper(frame.getContext(), dest);

    View incomingView = LayoutInflater.from(frame.getContext()) //
        .cloneInContext(screenContext) //
        .inflate(layout, frame, false);

    frame.addView(incomingView);
    traversal.destination.topViewState().restore(incomingView);

    callback.onTraversalCompleted();
  }
}

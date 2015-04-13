package com.example.flow.pathview;

import android.content.Context;
import android.util.AttributeSet;
import com.example.flow.R;
import flow.Flow;
import flow.path.Path;
import flow.path.PathContextFactory;

import static com.example.flow.Paths.MasterDetailPath;

public class MasterPathContainerView extends FramePathContainerView {

  public MasterPathContainerView(Context context, AttributeSet attrs) {
    super(context, attrs, new MasterPathContainer(R.id.screen_switcher_tag, Path.contextFactory()));
  }

  @Override public void dispatch(Flow.Traversal traversal, final Flow.TraversalCallback callback) {

    MasterDetailPath currentMaster =
        ((MasterDetailPath) Flow.get(getContext()).getBackstack().top()).getMaster();

    MasterDetailPath newMaster = ((MasterDetailPath) traversal.destination.top()).getMaster();

    // Short circuit if the new screen has the same master.
    if (getCurrentChild() != null && newMaster.equals(currentMaster)) {
      callback.onTraversalCompleted();
    } else {
      super.dispatch(traversal, new Flow.TraversalCallback() {
        @Override public void onTraversalCompleted() {
          callback.onTraversalCompleted();
        }
      });
    }
  }

  static class MasterPathContainer extends SimplePathContainer {

    MasterPathContainer(int tagKey, PathContextFactory contextFactory) {
      super(tagKey, contextFactory);
    }

    @Override protected int getLayout(Path path) {
      MasterDetailPath mdPath = (MasterDetailPath) path;
      return super.getLayout(mdPath.getMaster());
    }
  }
}

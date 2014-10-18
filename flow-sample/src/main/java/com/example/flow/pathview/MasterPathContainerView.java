package com.example.flow.pathview;

import android.content.Context;
import android.util.AttributeSet;
import com.example.flow.R;
import flow.Flow;
import flow.Path;
import flow.PathContainer;
import flow.PathContainerView;
import flow.PathContextFactory;

import static com.example.flow.Paths.MasterDetailPath;

public class MasterPathContainerView extends FramePathContainerView {

  public MasterPathContainerView(Context context, AttributeSet attrs) {
    super(context, attrs,
        new PathContainer.Factory(R.id.screen_switcher_tag, Path.contextFactory()) {
          @Override public PathContainer createPathContainer(PathContainerView view) {
            return new MasterPathContainer(view, tagKey, contextFactory);
          }
        });
  }

  @Override public void executeTraversal(Flow.Traversal traversal,
      final Flow.TraversalCallback callback) {

    MasterDetailPath currentMaster =
        ((MasterDetailPath) Flow.get(getContext()).getBackstack().current()).getMaster();

    MasterDetailPath newMaster = ((MasterDetailPath) traversal.destination.current()).getMaster();

    // Short circuit if the new screen has the same master.
    if (getCurrentChild() != null && newMaster.equals(currentMaster)) {
      callback.onTraversalCompleted();
    } else {
      super.executeTraversal(traversal, new Flow.TraversalCallback() {
        @Override public void onTraversalCompleted() {
          callback.onTraversalCompleted();
        }
      });
    }
  }

  static class MasterPathContainer extends SimplePathContainer {

    MasterPathContainer(PathContainerView view, int tagKey, PathContextFactory contextFactory) {
      super(view, tagKey, contextFactory);
    }

    @Override protected int getLayout(Path path) {
      MasterDetailPath mdPath = (MasterDetailPath) path;
      return super.getLayout(mdPath.getMaster());
    }
  }
}

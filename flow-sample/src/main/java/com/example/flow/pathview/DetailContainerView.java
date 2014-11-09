package com.example.flow.pathview;

import android.content.Context;
import android.util.AttributeSet;
import com.example.flow.Paths;
import com.example.flow.R;
import flow.Path;
import flow.PathContainer;
import flow.PathContainerView;
import flow.PathContextFactory;

import static com.example.flow.Paths.MasterDetailPath;

public class DetailContainerView extends FramePathContainerView {

  public DetailContainerView(Context context, AttributeSet attrs) {
    super(context, attrs,
        new PathContainer.Factory(R.id.screen_switcher_tag, Path.contextFactory()) {
          @Override public PathContainer createPathContainer(PathContainerView view) {
            return new DetailPathContainer(view, tagKey, contextFactory);
          }
        });
  }

  static class DetailPathContainer extends SimplePathContainer {
    DetailPathContainer(PathContainerView view, int tagKey, PathContextFactory contextFactory) {
      super(view, tagKey, contextFactory);
    }

    @Override protected int getLayout(Path path) {
      MasterDetailPath mdPath = (MasterDetailPath) path;
      return super.getLayout(mdPath.isMaster() ? new Paths.NoDetails() : mdPath);
    }
  }
}

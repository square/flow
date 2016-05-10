package flow;

import android.support.annotation.Nullable;

public final class EmptyTraversalTransformer implements TraversalTransformer {

  @Nullable @Override public Transform maybeTransform(Traversal proposedTraversal) {
    return null;
  }
}

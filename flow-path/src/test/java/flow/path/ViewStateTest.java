package flow.path;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import flow.Flow;
import flow.History;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This really belongs in Flow proper, but at the moment the only way to get at ViewState
 * is through PathContainer. Oops.
 */
@RunWith(RobolectricTestRunner.class) // Necessary for functional SparseArray
@Config(manifest = Config.NONE) //
public class ViewStateTest {
  TestScreen able = new TestScreen("able");
  TestScreen baker = new TestScreen("baker");

  @Test public void stuff() {
    // Suppose we start off with a flow instance in an portrait-oriented activity;
    Flow portrait = new Flow(History.single(able));
    portrait.setDispatcher(new Dispatcher());
    // We go from able to baker.
    portrait.set(baker);

    // Pretend we wrote the history to a bundle and read it out again.
    History history = portrait.getHistory();

    // A new landscape activity instance creates a new flow instance with the "restored" history.
    Flow landscape = new Flow(history);
    Dispatcher landscapeDispatcher = new Dispatcher();
    landscape.setDispatcher(landscapeDispatcher);
    // Back from baker to able.
    landscape.goBack();
    TestScreenView currentView = (TestScreenView) landscapeDispatcher.containerLayout.getChildAt(0);
    // We expect that able's view state will have been restored.
    assertThat(currentView.restoredName).isEqualTo(able.name);
  }

  static class Dispatcher implements Flow.Dispatcher {
    final TestContainer container = new TestContainer();

    final ViewGroup containerLayout = new FrameLayout(RuntimeEnvironment.application) {
      Object tag;

      // The real code insists on a real resource id, life is too short.
      @Override public void setTag(int key, Object tag) {
        this.tag = tag;
      }

      @Override public Object getTag(int key) {
        return tag;
      }
    };

    @Override public void dispatch(Flow.Traversal traversal, Flow.TraversalCallback callback) {
      container.executeFlowTraversal(containerLayout, traversal, callback);
    }
  }

  static class TestScreen extends Path {
    final String name;

    TestScreen(String name) {
      this.name = name;
    }

    View createView() {
      return new TestScreenView(name);
    }

    @Override public String toString() {
      return getClass().getSimpleName() + "-" + name;
    }

    @Override public final boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TestScreen that = (TestScreen) o;

      return name.equals(that.name);
    }

    @Override public final int hashCode() {
      return name.hashCode();
    }
  }

  @SuppressLint("ViewConstructor") //
  static class TestScreenView extends View {
    final String creationName;
    String restoredName;

    public TestScreenView(String name) {
      super(RuntimeEnvironment.application);
      this.creationName = name;

      // Have to have an id for instance state magic to fire.
      //noinspection ResourceType
      this.setId(1234);
    }

    @Override protected Parcelable onSaveInstanceState() {
      Parcelable supah = super.onSaveInstanceState();
      Bundle b = new Bundle();
      b.putParcelable("supah", supah);
      b.putString("name", creationName);
      return b;
    }

    @Override protected void onRestoreInstanceState(Parcelable state) {
      Bundle b = (Bundle) state;
      restoredName = b.getString("name");
      assertThat(restoredName).isEqualTo(creationName);
      super.onRestoreInstanceState(((Bundle) state).getParcelable("supah"));
    }
  }

  static class TestContainer extends PathContainer {
    protected TestContainer() {
      super(1234);
    }

    @Override
    protected void performTraversal(ViewGroup containerView, TraversalState traversalState,
        Flow.Direction direction, Flow.TraversalCallback callback) {

      Path to = traversalState.toPath();

      View newView;
      newView = ((TestScreen) to).createView();
      final View fromView = containerView.getChildAt(0);
      if (fromView != null) {
        traversalState.saveViewState(fromView);
      }
      traversalState.restoreViewState(newView);

      containerView.removeAllViews();
      containerView.addView(newView);
      callback.onTraversalCompleted();
    }
  }
}

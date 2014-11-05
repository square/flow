/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package flow;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static flow.Flow.Traversal;
import static flow.Flow.TraversalCallback;
import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;

public class FlowTest {
  static class Uno extends Path {
  }

  static class Dos extends Path implements HasParent {
    @Override public Uno getParent() {
      return new Uno();
    }
  }

  static class Tres extends Path implements HasParent {
    @Override public Dos getParent() {
      return new Dos();
    }
  }

  final TestPath able = new TestPath("Able");
  final TestPath baker = new TestPath("Baker");
  final TestPath charlie = new TestPath("Charlie");
  final TestPath delta = new TestPath("Delta");

  Backstack lastStack;
  Flow.Direction lastDirection;

  class FlowDispatcher implements Flow.Dispatcher {
    @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
      lastStack = traversal.destination;
      lastDirection = traversal.direction;
      callback.onTraversalCompleted();
    }
  }

  @Test public void oneTwoThree() {
    Backstack backstack = Backstack.single(new Uno());
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    flow.goTo(new Dos());
    assertThat(lastStack.current()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.FORWARD);

    flow.goTo(new Tres());
    assertThat(lastStack.current()).isInstanceOf(Tres.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.FORWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void builderCanPushPeekAndPopPaths() {
    Backstack.Builder builder = Backstack.emptyBuilder();

    List<TestPath> paths = asList(able, baker, charlie);
    for (Path path : paths) {
      builder.push(path);
    }

    for (int i = paths.size() - 1; i >= 0; i--) {
      Path path = paths.get(i);
      assertThat(builder.peek()).isSameAs(path);
      assertThat(builder.pop()).isSameAs(path);
    }
  }

  @Test public void backstackChangesAfterListenerCall() {
    final Backstack firstBackstack = Backstack.single(new Uno());

    class Ourrobouros implements Flow.Dispatcher {
      Flow flow = new Flow(firstBackstack);

      {
        flow.setDispatcher(this);
      }

      @Override public void dispatch(Traversal traversal, TraversalCallback onComplete) {
        assertThat(firstBackstack).isSameAs(flow.getBackstack());
        onComplete.onTraversalCompleted();
      }
    }

    Ourrobouros listener = new Ourrobouros();
    listener.flow.goTo(new Dos());
  }

  @Test public void noUpNoUps() {
    Backstack backstack = Backstack.single(new Uno());
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    lastStack = null;
    lastDirection = null;

    assertThat(flow.goUp()).isFalse();
    assertThat(lastStack).isNull();
    assertThat(lastDirection).isNull();
  }

  @Test public void upAndDown() {
    Backstack backstack = Backstack.single(new Tres());
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(flow.goBack()).isFalse();

    assertThat(flow.goUp()).isTrue();
    assertThat(lastStack.current()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goUp()).isTrue();
    assertThat(lastStack.current()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goUp()).isFalse();
  }

  @Test public void backStackAddAllIsPushy() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Path>asList(able, baker, charlie)).build();
    assertThat(backstack.size()).isEqualTo(3);

    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isEqualTo(baker);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isEqualTo(able);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void replaceBuildsBackStackFromUpLinks() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Path>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    flow.replaceTo(new Tres());
    assertThat(lastStack.current()).isInstanceOf(Tres.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.REPLACE);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void resetGoesBack() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Path>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(backstack.size()).isEqualTo(4);

    flow.resetTo(charlie);
    assertThat(lastStack.current()).isEqualTo(charlie);
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isEqualTo(baker);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isEqualTo(able);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void resetToMissingScreenPushes() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Path>asList(able, baker)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(backstack.size()).isEqualTo(2);

    flow.resetTo(charlie);
    assertThat(lastStack.current()).isEqualTo(charlie);
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.FORWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isEqualTo(baker);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isEqualTo(able);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);
    assertThat(flow.goBack()).isFalse();
  }

  @Test public void resetKeepsOriginal() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Path>asList(able, baker)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(backstack.size()).isEqualTo(2);

    flow.resetTo(new TestPath("Able"));
    assertThat(lastStack.current()).isEqualTo(new TestPath("Able"));
    assertThat(lastStack.current() == able).isTrue();
    assertThat(lastStack.current()).isSameAs(able);
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);
  }

  @Test public void replaceKeepsOriginals() {
    TestPath able = new Grandpa();
    TestPath baker = new Daddy();
    TestPath charlie = new Baby();
    TestPath delta = new TestPath("Delta");
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Path>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(backstack.size()).isEqualTo(4);

    TestPath foxtrot = new Foxtrot();
    flow.replaceTo(foxtrot);
    assertThat(lastStack.size()).isEqualTo(4);
    assertThat(lastStack.current()).isSameAs(foxtrot);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastStack.current()).isEqualTo(new Echo());
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(2);
    assertThat(lastStack.current()).isSameAs(baker);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastStack.current()).isSameAs(able);
  }

  @Test public void goUpKeepsOriginals() {
    TestPath able = new Grandpa();
    TestPath baker = new Daddy();
    TestPath charlie = new Baby();
    TestPath delta = new TestPath("Delta");
    TestPath foxtrot = new Foxtrot();

    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Path>asList(able, baker, charlie, delta, foxtrot))
        .build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(backstack.size()).isEqualTo(5);

    flow.goUp();
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastStack.current()).isEqualTo(new Echo());
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(2);
    assertThat(lastStack.current()).isSameAs(baker);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastStack.current()).isSameAs(able);
  }

  static class Picky extends Path {
    final String value;

    Picky(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Picky picky = (Picky) o;
      return value.equals(picky.value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  @Test public void resetCallsEquals() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Path>asList(new Picky("Able"), new Picky("Baker"), new Picky("Charlie"),
            new Picky("Delta")))
        .build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(backstack.size()).isEqualTo(4);

    flow.resetTo(new Picky("Charlie"));
    assertThat(lastStack.current()).isEqualTo(new Picky("Charlie"));
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isEqualTo(new Picky("Baker"));
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isEqualTo(new Picky("Able"));
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void replaceWithNonUppy() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Path>asList(new Picky("Able"), new Picky("Baker"), new Picky("Charlie"),
            new Picky("Delta")))
        .build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    flow.replaceTo(new TestPath("Echo"));
    Backstack newBack = flow.getBackstack();
    assertThat(newBack.size()).isEqualTo(1);
    assertThat(newBack.current()).isEqualTo(new TestPath("Echo"));
  }

  /**
   * Sometimes its nice to jump into a new flow at a midpoint.
   */
  @Test public void buildFromUp() {
    Backstack backstack = Backstack.fromUpChain(new Tres());
    assertThat(backstack.size()).isEqualTo(3);

    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(flow.getBackstack().current()).isInstanceOf(Tres.class);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  static class Grandpa extends TestPath {
    Grandpa() {
      super("Grandpa");
    }
  }

  static class Daddy extends TestPath implements HasParent {
    Daddy() {
      super("Daddy");
    }

    @Override public TestPath getParent() {
      return new Grandpa();
    }
  }

  static class Baby extends TestPath implements HasParent {
    Baby() {
      super("Baby");
    }

    @Override public TestPath getParent() {
      return new Daddy();
    }
  }

  static class Echo extends TestPath implements HasParent {
    Echo() {
      super("Echo");
    }

    @Override public TestPath getParent() {
      return new Daddy();
    }
  }

  static class Foxtrot extends TestPath implements HasParent {
    Foxtrot() {
      super("Foxtrot");
    }

    @Override public TestPath getParent() {
      return new Echo();
    }
  }
}

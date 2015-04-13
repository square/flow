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
  static class Uno extends Object {
  }

  @SuppressWarnings("deprecation")
  static class Dos extends Object implements HasParent {
    @Override public Uno getParent() {
      return new Uno();
    }
  }

  @SuppressWarnings("deprecation")
  static class Tres extends Object implements HasParent {
    @Override public Dos getParent() {
      return new Dos();
    }
  }

  final TestState able = new TestState("Able");
  final TestState baker = new TestState("Baker");
  final TestState charlie = new TestState("Charlie");
  final TestState delta = new TestState("Delta");

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

    flow.set(new Dos());
    assertThat(lastStack.top()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.FORWARD);

    flow.set(new Tres());
    assertThat(lastStack.top()).isInstanceOf(Tres.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.FORWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void builderCanPushPeekAndPopObjects() {
    Backstack.Builder builder = Backstack.emptyBuilder();

    List<TestState> objects = asList(able, baker, charlie);
    for (Object object : objects) {
      builder.push(object);
    }

    for (int i = objects.size() - 1; i >= 0; i--) {
      Object object = objects.get(i);
      assertThat(builder.peek()).isSameAs(object);
      assertThat(builder.pop()).isSameAs(object);
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
    listener.flow.set(new Dos());
  }

  @Test public void noUpNoUps() {
    Backstack backstack = Backstack.single(new Uno());
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    lastStack = null;
    lastDirection = null;

    //noinspection deprecation
    assertThat(flow.goUp()).isFalse();
    assertThat(lastStack).isNull();
    assertThat(lastDirection).isNull();
  }

  @SuppressWarnings("deprecation")
  @Test public void upAndDown() {
    Backstack backstack = Backstack.single(new Tres());
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(flow.goBack()).isFalse();

    assertThat(flow.goUp()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goUp()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goUp()).isFalse();
  }

  @Test public void backStackAddAllIsPushy() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(able, baker, charlie)).build();
    assertThat(backstack.size()).isEqualTo(3);

    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(baker);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(able);

    assertThat(flow.goBack()).isFalse();
  }

  @SuppressWarnings("deprecation") @Test public void replaceBuildsBackStackFromUpLinks() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    flow.replaceTo(new Tres());
    assertThat(lastStack.top()).isInstanceOf(Tres.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.REPLACE);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setBackstackWorks() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(backstack);
    FlowDispatcher dispatcher = new FlowDispatcher();
    flow.setDispatcher(dispatcher);

    Backstack newBackstack = Backstack.emptyBuilder().addAll(
        Arrays.<Object>asList(charlie, delta)).build();
    flow.setBackstack(newBackstack, Flow.Direction.FORWARD);
    assertThat(lastDirection).isSameAs(Flow.Direction.FORWARD);
    assertThat(lastStack.top()).isSameAs(delta);
    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isSameAs(charlie);
    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setObjectGoesBack() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(backstack.size()).isEqualTo(4);

    flow.set(charlie);
    assertThat(lastStack.top()).isEqualTo(charlie);
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(baker);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(able);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setObjectToMissingObjectPushes() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(backstack.size()).isEqualTo(2);

    flow.set(charlie);
    assertThat(lastStack.top()).isEqualTo(charlie);
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.FORWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(baker);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(able);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);
    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setObjectKeepsOriginal() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(backstack.size()).isEqualTo(2);

    flow.set(new TestState("Able"));
    assertThat(lastStack.top()).isEqualTo(new TestState("Able"));
    assertThat(lastStack.top() == able).isTrue();
    assertThat(lastStack.top()).isSameAs(able);
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);
  }

  @SuppressWarnings("deprecation") @Test public void replaceKeepsOriginals() {
    TestState able = new Grandpa();
    TestState baker = new Daddy();
    TestState charlie = new Baby();
    TestState delta = new TestState("Delta");
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(backstack.size()).isEqualTo(4);

    TestState foxtrot = new Foxtrot();
    flow.replaceTo(foxtrot);
    assertThat(lastStack.size()).isEqualTo(4);
    assertThat(lastStack.top()).isSameAs(foxtrot);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastStack.top()).isEqualTo(new Echo());
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(2);
    assertThat(lastStack.top()).isSameAs(baker);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastStack.top()).isSameAs(able);
  }

  @SuppressWarnings("deprecation") @Test public void goUpKeepsOriginals() {
    TestState able = new Grandpa();
    TestState baker = new Daddy();
    TestState charlie = new Baby();
    TestState delta = new TestState("Delta");
    TestState foxtrot = new Foxtrot();

    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList(able, baker, charlie, delta, foxtrot))
        .build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(backstack.size()).isEqualTo(5);

    flow.goUp();
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastStack.top()).isEqualTo(new Echo());
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(2);
    assertThat(lastStack.top()).isSameAs(baker);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastStack.top()).isSameAs(able);
  }

  static class Picky extends Object {
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

  @Test public void setCallsEquals() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList(new Picky("Able"), new Picky("Baker"), new Picky("Charlie"),
            new Picky("Delta")))
        .build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(backstack.size()).isEqualTo(4);

    flow.set(new Picky("Charlie"));
    assertThat(lastStack.top()).isEqualTo(new Picky("Charlie"));
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(new Picky("Baker"));
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(new Picky("Able"));
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @SuppressWarnings("deprecation") @Test public void replaceWithNonUppy() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList(new Picky("Able"), new Picky("Baker"), new Picky("Charlie"),
            new Picky("Delta")))
        .build();
    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());

    flow.replaceTo(new TestState("Echo"));
    Backstack newBack = flow.getBackstack();
    assertThat(newBack.size()).isEqualTo(1);
    assertThat(newBack.top()).isEqualTo(new TestState("Echo"));
  }

  /**
   * Sometimes its nice to jump into a new flow at a midpoint.
   */
  @SuppressWarnings("deprecation") @Test public void buildFromUp() {
    Backstack backstack = Backstack.fromUpChain(new Tres());
    assertThat(backstack.size()).isEqualTo(3);

    Flow flow = new Flow(backstack);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(flow.getBackstack().top()).isInstanceOf(Tres.class);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  static class Grandpa extends TestState {
    Grandpa() {
      super("Grandpa");
    }
  }

  @SuppressWarnings("deprecation")
  static class Daddy extends TestState implements HasParent {
    Daddy() {
      super("Daddy");
    }

    @Override public TestState getParent() {
      return new Grandpa();
    }
  }

  @SuppressWarnings("deprecation")
  static class Baby extends TestState implements HasParent {
    Baby() {
      super("Baby");
    }

    @Override public TestState getParent() {
      return new Daddy();
    }
  }

  @SuppressWarnings("deprecation")
  static class Echo extends TestState implements HasParent {
    Echo() {
      super("Echo");
    }

    @Override public TestState getParent() {
      return new Daddy();
    }
  }

  @SuppressWarnings("deprecation")
  static class Foxtrot extends TestState implements HasParent {
    Foxtrot() {
      super("Foxtrot");
    }

    @Override public TestState getParent() {
      return new Echo();
    }
  }
}

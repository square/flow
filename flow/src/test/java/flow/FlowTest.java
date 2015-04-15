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
  static class Uno {
  }

  static class Dos {
  }

  static class Tres {
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

  @Test public void setBackstackWorks() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(backstack);
    FlowDispatcher dispatcher = new FlowDispatcher();
    flow.setDispatcher(dispatcher);

    Backstack newBackstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList(charlie, delta)).build();
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

  static class Picky {
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
}

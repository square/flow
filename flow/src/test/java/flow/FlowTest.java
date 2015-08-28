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
import java.util.Iterator;
import java.util.List;
import org.junit.Test;

import static flow.Flow.Traversal;
import static flow.Flow.TraversalCallback;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

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

  History lastStack;
  Flow.Direction lastDirection;

  class FlowDispatcher implements Flow.Dispatcher {
    @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
      lastStack = traversal.destination;
      lastDirection = traversal.direction;
      callback.onTraversalCompleted();
    }
  }

  @Test public void oneTwoThree() {
    History history = History.single(new Uno());
    Flow flow = new Flow(history);
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
    History.Builder builder = History.emptyBuilder();

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

  @Test public void historyChangesAfterListenerCall() {
    final History firstHistory = History.single(new Uno());

    class Ourrobouros implements Flow.Dispatcher {
      Flow flow = new Flow(firstHistory);

      {
        flow.setDispatcher(this);
      }

      @Override public void dispatch(Traversal traversal, TraversalCallback onComplete) {
        assertThat(firstHistory).hasSameSizeAs(flow.getHistory());
        Iterator<Object> original = firstHistory.iterator();
        Iterator<Object> current = flow.getHistory().iterator();
        while (current.hasNext()) {
          assertThat(current.next()).isEqualTo(original.next());
        }
        onComplete.onTraversalCompleted();
      }
    }

    Ourrobouros listener = new Ourrobouros();
    listener.flow.set(new Dos());
  }

  @Test public void historyAddAllIsPushy() {
    History history =
        History.emptyBuilder().addAll(Arrays.<Object>asList(able, baker, charlie)).build();
    assertThat(history.size()).isEqualTo(3);

    Flow flow = new Flow(history);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(baker);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(able);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setHistoryWorks() {
    History history =
        History.emptyBuilder().addAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(history);
    FlowDispatcher dispatcher = new FlowDispatcher();
    flow.setDispatcher(dispatcher);

    History newHistory =
        History.emptyBuilder().addAll(Arrays.<Object>asList(charlie, delta)).build();
    flow.setHistory(newHistory, Flow.Direction.FORWARD);
    assertThat(lastDirection).isSameAs(Flow.Direction.FORWARD);
    assertThat(lastStack.top()).isSameAs(delta);
    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isSameAs(charlie);
    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setObjectGoesBack() {
    History history =
        History.emptyBuilder().addAll(Arrays.<Object>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(history);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(history.size()).isEqualTo(4);

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
    History history =
        History.emptyBuilder().addAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(history);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(history.size()).isEqualTo(2);

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
    History history =
        History.emptyBuilder().addAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(history);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(history.size()).isEqualTo(2);

    flow.set(new TestState("Able"));
    assertThat(lastStack.top()).isEqualTo(new TestState("Able"));
    assertThat(lastStack.top() == able).isTrue();
    assertThat(lastStack.top()).isSameAs(able);
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);
  }

  @SuppressWarnings("deprecation") @Test public void setHistoryKeepsOriginals() {
    TestState able = new TestState("Able");
    TestState baker = new TestState("Baker");
    TestState charlie = new TestState("Charlie");
    TestState delta = new TestState("Delta");
    History history =
        History.emptyBuilder().addAll(Arrays.<Object>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(history);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(history.size()).isEqualTo(4);

    TestState echo = new TestState("Echo");
    TestState foxtrot = new TestState("Foxtrot");
    History newHistory =
        History.emptyBuilder().addAll(Arrays.<Object>asList(able, baker, echo, foxtrot)).build();
    flow.setHistory(newHistory, Flow.Direction.REPLACE);
    assertThat(lastStack.size()).isEqualTo(4);
    assertThat(lastStack.top()).isEqualTo(foxtrot);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastStack.top()).isEqualTo(echo);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(2);
    assertThat(lastStack.top()).isSameAs(baker);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastStack.top()).isSameAs(able);
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
    History history = History.emptyBuilder()
        .addAll(Arrays.<Object>asList(new Picky("Able"), new Picky("Baker"), new Picky("Charlie"),
            new Picky("Delta")))
        .build();
    Flow flow = new Flow(history);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(history.size()).isEqualTo(4);

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

  @Test public void emptyBuilderPeekIsNullable() {
    assertThat(History.emptyBuilder().peek()).isNull();
  }

  @Test public void emptyBuilderPopThrows() {
    try {
      History.emptyBuilder().pop();
      fail("Should throw");
    } catch (IllegalStateException e) {
      // pass
    }
  }

  @Test public void isEmpty() {
    final History.Builder builder = History.emptyBuilder();
    assertThat(builder.isEmpty()).isTrue();
    builder.push("foo");
    assertThat(builder.isEmpty()).isFalse();
    builder.pop();
    assertThat(builder.isEmpty()).isTrue();
  }
}

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

import android.content.Context;
import android.support.annotation.NonNull;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.MockitoAnnotations.initMocks;

public class FlowTest {
  static class Uno {
  }

  static class Dos {
  }

  static class Tres {
  }

  final TestKey able = new TestKey("Able");
  final TestKey baker = new TestKey("Baker");
  final TestKey charlie = new TestKey("Charlie");
  final TestKey delta = new TestKey("Delta");

  @Mock KeyManager keyManager;
  History lastStack;
  Direction lastDirection;

  class FlowDispatcher implements Dispatcher {
    @Override
    public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
      lastStack = traversal.destination;
      lastDirection = traversal.direction;
      callback.onTraversalCompleted();
    }
  }

  class AsyncDispatcher implements Dispatcher {
    Traversal traversal;
    TraversalCallback callback;

    @Override
    public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
      this.traversal = traversal;
      this.callback = callback;
    }

    void fire() {
      TraversalCallback oldCallback = callback;
      callback = null;
      traversal = null;
      oldCallback.onTraversalCompleted();;
    }

    void assertIdle() {
      assertThat(callback).isNull();
      assertThat(traversal).isNull();
    }

    void assertDispatching(Object newTop) {
      assertThat(callback).isNotNull();
      assertThat(traversal.destination.top()).isEqualTo(newTop);
    }
  }

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void oneTwoThree() {
    History history = History.single(new Uno());
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());

    flow.set(new Dos());
    assertThat(lastStack.top()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Direction.FORWARD);

    flow.set(new Tres());
    assertThat(lastStack.top()).isInstanceOf(Tres.class);
    assertThat(lastDirection).isSameAs(Direction.FORWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void historyChangesAfterListenerCall() {
    final History firstHistory = History.single(new Uno());

    class Ourrobouros implements Dispatcher {
      Flow flow = new Flow(keyManager, firstHistory);

      {
        flow.setDispatcher(this);
      }

      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback onComplete) {
        assertThat(firstHistory).hasSameSizeAs(flow.getHistory());
        Iterator<Object> original = firstHistory.iterator();
        for (Object o : flow.getHistory()) {
          assertThat(o).isEqualTo(original.next());
        }
        onComplete.onTraversalCompleted();
      }
    }

    Ourrobouros listener = new Ourrobouros();
    listener.flow.set(new Dos());
  }

  @Test public void historyPushAllIsPushy() {
    History history =
        History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker, charlie)).build();
    assertThat(history.size()).isEqualTo(3);

    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(baker);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(able);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setHistoryWorks() {
    History history = History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(keyManager, history);
    FlowDispatcher dispatcher = new FlowDispatcher();
    flow.setDispatcher(dispatcher);

    History newHistory =
        History.emptyBuilder().pushAll(Arrays.<Object>asList(charlie, delta)).build();
    flow.setHistory(newHistory, Direction.FORWARD);
    assertThat(lastDirection).isSameAs(Direction.FORWARD);
    assertThat(lastStack.top()).isSameAs(delta);
    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isSameAs(charlie);
    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setObjectGoesBack() {
    History history =
        History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(history.size()).isEqualTo(4);

    flow.set(charlie);
    assertThat(lastStack.top()).isEqualTo(charlie);
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(baker);
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(able);
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setObjectToMissingObjectPushes() {
    History history = History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(history.size()).isEqualTo(2);

    flow.set(charlie);
    assertThat(lastStack.top()).isEqualTo(charlie);
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Direction.FORWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(baker);
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(able);
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);
    assertThat(flow.goBack()).isFalse();
  }

  @Test public void setObjectKeepsOriginal() {
    History history = History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker)).build();
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(history.size()).isEqualTo(2);

    flow.set(new TestKey("Able"));
    assertThat(lastStack.top()).isEqualTo(new TestKey("Able"));
    assertThat(lastStack.top() == able).isTrue();
    assertThat(lastStack.top()).isSameAs(able);
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);
  }

  @Test public void replaceHistoryResultsInLengthOneHistory() {
    History history =
        History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker, charlie)).build();
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(history.size()).isEqualTo(3);

    flow.replaceHistory(delta, Direction.REPLACE);
    assertThat(lastStack.top()).isEqualTo(new TestKey("Delta"));
    assertThat(lastStack.top() == delta).isTrue();
    assertThat(lastStack.top()).isSameAs(delta);
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastDirection).isEqualTo(Direction.REPLACE);
  }

  @Test public void replaceTopDoesNotAlterHistoryLength() {
    History history =
        History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker, charlie)).build();
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(history.size()).isEqualTo(3);

    flow.replaceTop(delta, Direction.REPLACE);
    assertThat(lastStack.top()).isEqualTo(new TestKey("Delta"));
    assertThat(lastStack.top() == delta).isTrue();
    assertThat(lastStack.top()).isSameAs(delta);
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Direction.REPLACE);
  }

  @Test public void secondDispatcherIsBootstrapped() {
    AsyncDispatcher firstDispatcher = new AsyncDispatcher();

    History history = History.single(able);
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(firstDispatcher);

    // Quick check that we bootstrapped (and test the test dispatcher).
    firstDispatcher.assertDispatching(able);
    firstDispatcher.fire();
    firstDispatcher.assertIdle();

    // No activity, dispatchers change. Maybe pause / resume. Maybe config change.
    flow.removeDispatcher(firstDispatcher);
    AsyncDispatcher secondDispatcher = new AsyncDispatcher();
    flow.setDispatcher(secondDispatcher);

    // New dispatcher is bootstrapped
    secondDispatcher.assertDispatching(able);
    secondDispatcher.fire();
    secondDispatcher.assertIdle();
  }

  @Test public void hangingTraversalsSurviveDispatcherChange() {
    AsyncDispatcher firstDispatcher = new AsyncDispatcher();

    History history = History.single(able);
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(firstDispatcher);
    firstDispatcher.fire();

    // Start traversal to second screen.
    flow.set(baker);
    firstDispatcher.assertDispatching(baker);

    // Dispatcher is removed before finishing baker--maybe it caused a configuration change.
    flow.removeDispatcher(firstDispatcher);

    // New dispatcher shows up, maybe from new activity after config change.
    AsyncDispatcher secondDispatcher = new AsyncDispatcher();
    flow.setDispatcher(secondDispatcher);

    // New dispatcher is ignored until the in-progress baker traversal is done.
    secondDispatcher.assertIdle();

    // New dispatcher is bootstrapped with baker.
    firstDispatcher.fire();
    secondDispatcher.assertDispatching(baker);

    // Confirm no redundant extra bootstrap traversals enqueued.
    secondDispatcher.fire();
    secondDispatcher.assertIdle();
  }

  @Test public void enqueuedTraversalsSurviveDispatcherChange() {
    AsyncDispatcher firstDispatcher = new AsyncDispatcher();

    History history = History.single(able);
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(firstDispatcher);
    firstDispatcher.fire();

    // Dispatcher is removed. Maybe we paused.
    flow.removeDispatcher(firstDispatcher);

    // A few traversals are enqueued because software.
    flow.set(baker);
    flow.set(charlie);

    // New dispatcher shows up, we resumed.
    AsyncDispatcher secondDispatcher = new AsyncDispatcher();
    flow.setDispatcher(secondDispatcher);

    // New dispatcher receives baker and charlie traversals and nothing else.
    secondDispatcher.assertDispatching(baker);
    secondDispatcher.fire();
    secondDispatcher.assertDispatching(charlie);
    secondDispatcher.fire();
    secondDispatcher.assertIdle();
  }

  @SuppressWarnings({ "deprecation", "CheckResult" }) @Test public void setHistoryKeepsOriginals() {
    TestKey able = new TestKey("Able");
    TestKey baker = new TestKey("Baker");
    TestKey charlie = new TestKey("Charlie");
    TestKey delta = new TestKey("Delta");
    History history =
        History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker, charlie, delta)).build();
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());
    assertThat(history.size()).isEqualTo(4);

    TestKey echo = new TestKey("Echo");
    TestKey foxtrot = new TestKey("Foxtrot");
    History newHistory =
        History.emptyBuilder().pushAll(Arrays.<Object>asList(able, baker, echo, foxtrot)).build();
    flow.setHistory(newHistory, Direction.REPLACE);
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

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Picky picky = (Picky) o;
      return value.equals(picky.value);
    }

    @Override public int hashCode() {
      return value.hashCode();
    }
  }

  @Test public void setCallsEquals() {
    History history = History.emptyBuilder()
        .pushAll(Arrays.<Object>asList(new Picky("Able"), new Picky("Baker"), new Picky("Charlie"),
            new Picky("Delta")))
        .build();
    Flow flow = new Flow(keyManager, history);
    flow.setDispatcher(new FlowDispatcher());

    assertThat(history.size()).isEqualTo(4);

    flow.set(new Picky("Charlie"));
    assertThat(lastStack.top()).isEqualTo(new Picky("Charlie"));
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(new Picky("Baker"));
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.top()).isEqualTo(new Picky("Able"));
    assertThat(lastDirection).isEqualTo(Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void incorrectFlowGetUsage() {
    Context mockContext = Mockito.mock(Context.class);
    //noinspection WrongConstant
    Mockito.when(mockContext.getSystemService(Mockito.anyString())).thenReturn(null);

    try {
      Flow.get(mockContext);

      fail("Flow was supposed to throw an exception on wrong usage");
    } catch (IllegalStateException ignored) {
      // That's good!
    }
  }
}

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
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class FlowTest {
  static class Uno {
  }

  static class Dos implements HasParent<Uno> {
    @Override public Uno getParent() {
      return new Uno();
    }
  }

  static class Tres implements HasParent<Dos> {
    @Override public Dos getParent() {
      return new Dos();
    }
  }

  Backstack lastStack;
  Flow.Direction lastDirection;

  class FlowListener implements Flow.Listener {
    @Override public void go(Backstack nextBackstack, Flow.Direction direction, Flow.Callback callback) {
      lastStack = nextBackstack;
      lastDirection = direction;
      callback.onComplete();
    }
  }

  @Test public void oneTwoThree() {
    Backstack backstack = Backstack.single(new Uno());
    Flow flow = new Flow(backstack, new FlowListener());

    flow.goTo(new Dos());
    assertThat(lastStack.current().getScreen()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.FORWARD);

    flow.goTo(new Tres());
    assertThat(lastStack.current().getScreen()).isInstanceOf(Tres.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.FORWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void backstackChangesAfterListenerCall() {
    final Backstack firstBackstack = Backstack.single(new Uno());

    class Ourrobouros implements Flow.Listener {
      Flow flow = new Flow(firstBackstack ,this);

      @Override public void go(Backstack nextBackstack, Flow.Direction direction, Flow.Callback onComplete) {
        assertThat(firstBackstack).isSameAs(flow.getBackstack());
        onComplete.onComplete();
      }
    }

    Ourrobouros listener = new Ourrobouros();
    listener.flow.goTo(new Dos());
  }

  @Test public void noUpNoUps() {
    Backstack backstack = Backstack.single(new Uno());
    Flow flow = new Flow(backstack, new FlowListener());
    assertThat(flow.goUp()).isFalse();
    assertThat(lastStack).isNull();
    assertThat(lastDirection).isNull();
  }

  @Test public void upAndDown() {
    Backstack backstack = Backstack.single(new Tres());
    Flow flow = new Flow(backstack, new FlowListener());

    assertThat(flow.goBack()).isFalse();

    assertThat(flow.goUp()).isTrue();
    assertThat(lastStack.current().getScreen()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goUp()).isTrue();
    assertThat(lastStack.current().getScreen()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goUp()).isFalse();
  }

  @Test public void backStackAddAllIsPushy() {
    Backstack backstack =
        Backstack.emptyBuilder().addAll(Arrays.<Object>asList("Able", "Baker", "Charlie")).build();
    assertThat(backstack.size()).isEqualTo(3);

    Flow flow = new Flow(backstack, new FlowListener());

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isEqualTo("Baker");

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isEqualTo("Able");

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void replaceBuildsBackStackFromUpLinks() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList("Able", "Baker", "Charlie", "Delta"))
        .build();
    Flow flow = new Flow(backstack, new FlowListener());

    flow.replaceTo(new Tres());
    assertThat(lastStack.current().getScreen()).isInstanceOf(Tres.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.REPLACE);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void resetGoesBack() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList("Able", "Baker", "Charlie", "Delta"))
        .build();
    Flow flow = new Flow(backstack, new FlowListener());

    assertThat(backstack.size()).isEqualTo(4);

    flow.resetTo("Charlie");
    assertThat(lastStack.current().getScreen()).isEqualTo("Charlie");
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isEqualTo("Baker");
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isEqualTo("Able");
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void resetToMissingScreenPushes() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList("Able", "Baker"))
        .build();
    Flow flow = new Flow(backstack, new FlowListener());
    assertThat(backstack.size()).isEqualTo(2);

    flow.resetTo("Charlie");
    assertThat(lastStack.current().getScreen()).isEqualTo("Charlie");
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.FORWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isEqualTo("Baker");
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isEqualTo("Able");
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);
    assertThat(flow.goBack()).isFalse();
  }

  @Test public void resetKeepsOriginal() {
    TestScreen able = new TestScreen("Able");
    TestScreen baker = new TestScreen("Baker");
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList(able, baker))
        .build();
    Flow flow = new Flow(backstack, new FlowListener());
    assertThat(backstack.size()).isEqualTo(2);

    flow.resetTo(new TestScreen("Able"));
    assertThat(lastStack.current().getScreen()).isEqualTo(new TestScreen("Able"));
    assertThat(lastStack.current().getScreen() == able).isTrue();
    assertThat(lastStack.current().getScreen()).isSameAs(able);
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);
  }

  @Test public void replaceKeepsOriginals() {
    TestScreen able = new Able();
    TestScreen baker = new Baker();
    TestScreen charlie = new Charlie();
    TestScreen delta = new TestScreen("Delta");
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList(able, baker, charlie, delta))
        .build();
    Flow flow = new Flow(backstack, new FlowListener());
    assertThat(backstack.size()).isEqualTo(4);

    TestScreen foxtrot = new Foxtrot();
    flow.replaceTo(foxtrot);
    assertThat(lastStack.size()).isEqualTo(4);
    assertThat(lastStack.current().getScreen()).isSameAs(foxtrot);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastStack.current().getScreen()).isEqualTo(new Echo());
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(2);
    assertThat(lastStack.current().getScreen()).isSameAs(baker);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastStack.current().getScreen()).isSameAs(able);
  }

  @Test public void goUpKeepsOriginals() {
    TestScreen able = new Able();
    TestScreen baker = new Baker();
    TestScreen charlie = new Charlie();
    TestScreen delta = new TestScreen("Delta");
    TestScreen foxtrot = new Foxtrot();

    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList(able, baker, charlie, delta, foxtrot))
        .build();
    Flow flow = new Flow(backstack, new FlowListener());
    assertThat(backstack.size()).isEqualTo(5);

    flow.goUp();
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastStack.current().getScreen()).isEqualTo(new Echo());
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(2);
    assertThat(lastStack.current().getScreen()).isSameAs(baker);
    flow.goBack();
    assertThat(lastStack.size()).isEqualTo(1);
    assertThat(lastStack.current().getScreen()).isSameAs(able);
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

  @Test public void resetCallsEquals() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList(new Picky("Able"), new Picky("Baker"), new Picky("Charlie"),
            new Picky("Delta")))
        .build();
    Flow flow = new Flow(backstack, new FlowListener());

    assertThat(backstack.size()).isEqualTo(4);

    flow.resetTo(new Picky("Charlie"));
    assertThat(lastStack.current().getScreen()).isEqualTo(new Picky("Charlie"));
    assertThat(lastStack.size()).isEqualTo(3);
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isEqualTo(new Picky("Baker"));
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isEqualTo(new Picky("Able"));
    assertThat(lastDirection).isEqualTo(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  @Test public void replaceWithNonUppy() {
    Backstack backstack = Backstack.emptyBuilder()
        .addAll(Arrays.<Object>asList(new Picky("Able"), new Picky("Baker"), new Picky("Charlie"),
            new Picky("Delta")))
        .build();
    Flow flow = new Flow(backstack, new FlowListener());

    flow.replaceTo("Echo");
    Backstack newBack = flow.getBackstack();
    assertThat(newBack.size()).isEqualTo(1);
    assertThat(newBack.current().getScreen()).isEqualTo("Echo");
  }

  /**
   * Sometimes its nice to jump into a new flow at a midpoint.
   */
  @Test public void buildFromUp() {
    Backstack backstack = Backstack.fromUpChain(new Tres());
    assertThat(backstack.size()).isEqualTo(3);

    Flow flow = new Flow(backstack, new FlowListener());
    assertThat(flow.getBackstack().current().getScreen()).isInstanceOf(Tres.class);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isInstanceOf(Dos.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isTrue();
    assertThat(lastStack.current().getScreen()).isInstanceOf(Uno.class);
    assertThat(lastDirection).isSameAs(Flow.Direction.BACKWARD);

    assertThat(flow.goBack()).isFalse();
  }

  static class Able extends TestScreen {
    Able() {
      super("Able");
    }
  }

  static class Baker extends TestScreen implements HasParent<TestScreen> {
    Baker() {
      super("Baker");
    }

    @Override public TestScreen getParent() {
      return new Able();
    }
  }

  static class Charlie extends TestScreen implements HasParent<TestScreen> {
    Charlie() {
      super("Charlie");
    }
    @Override public TestScreen getParent() {
      return new Baker();
    }
  }


  static class Echo extends TestScreen implements HasParent<TestScreen> {
    Echo() {
      super("Echo");
    }
    @Override public TestScreen getParent() {
      return new Baker();
    }
  }

  static class Foxtrot extends TestScreen implements HasParent<TestScreen> {
    Foxtrot() {
      super("Foxtrot");
    }
    @Override public TestScreen getParent() {
      return new Echo();
    }
  }
}

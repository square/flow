/*
 * Copyright 2014 Square Inc.
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

import java.util.ArrayList;
import java.util.List;
import org.fest.assertions.api.Assertions;
import org.junit.Test;

import static flow.Flow.Traversal;
import static flow.Flow.TraversalCallback;
import static org.fest.assertions.api.Assertions.assertThat;

public class ReentranceTest {

  Flow flow;
  Backstack lastStack;
  TraversalCallback lastCallback;

  @Test public void reentrantGo() {
    Flow.Dispatcher dispatcher = new Flow.Dispatcher() {
      @Override public void dispatch(Traversal navigation, TraversalCallback callback) {
        lastStack = navigation.destination;
        Object next = navigation.destination.current().getScreen();
        if (next instanceof Detail) {
          flow.goTo(new Loading());
        } else if (next instanceof Loading) {
          flow.goTo(new Error());
        }
        callback.onTraversalCompleted();
      }
    };
    flow = new Flow(Backstack.single(new Catalog()), dispatcher);
    flow.goTo(new Detail());
    verifyBackstack(lastStack, new Error(), new Loading(), new Detail(), new Catalog());
  }

  @Test public void reentrantGoThenBack() {
    Flow.Dispatcher dispatcher = new Flow.Dispatcher() {
      boolean loading = true;
      @Override public void dispatch(Traversal navigation, TraversalCallback onComplete) {
        lastStack = navigation.destination;
        Object next = navigation.destination.current().getScreen();
        if (loading) {
          if (next instanceof Detail) {
            flow.goTo(new Loading());
          } else if (next instanceof Loading) {
            flow.goTo(new Error());
          } else if (next instanceof Error) {
            loading = false;
            flow.goBack();
          }
        } else {
          if (next instanceof Loading) {
            ReentranceTest.this.flow.goBack();
          }
        }
      }
    };
    flow = new Flow(Backstack.single(new Catalog()), dispatcher);
    flow.goTo(new Detail());
    verifyBackstack(lastStack, new Detail(), new Catalog());
  }

  @Test public void reentrantForwardThenGo() {
    Flow flow = new Flow(Backstack.single(new Catalog()), new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        Object next = traversal.destination.current().getScreen();
        if (next instanceof Detail) {
          ReentranceTest.this.flow.forward(Backstack.emptyBuilder()
              .push(new Detail())
              .push(new Loading())
              .build());
        } else if (next instanceof Loading) {
          ReentranceTest.this.flow.goTo(new Error());
        }
        callback.onTraversalCompleted();
      }
    });
    this.flow = flow;
    flow.goTo(new Detail());
    verifyBackstack(lastStack, new Error(), new Loading(), new Detail());
  }

  @Test public void reentranceWaitsForCallback() {
    Flow.Dispatcher dispatcher = new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        lastCallback = callback;
        Object next = traversal.destination.current().getScreen();
        if (next instanceof Detail) {
          flow.goTo(new Loading());
        } else if (next instanceof Loading) {
          flow.goTo(new Error());
        }
      }
    };
    flow = new Flow(Backstack.single(new Catalog()), dispatcher);
    flow.goTo(new Detail());
    verifyBackstack(flow.getBackstack(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyBackstack(flow.getBackstack(), new Detail(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyBackstack(flow.getBackstack(), new Loading(), new Detail(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyBackstack(flow.getBackstack(), new Error(), new Loading(), new Detail(), new Catalog());
  }

  @Test public void onCompleteThrowsIfCalledTwice() {
    flow = new Flow(Backstack.single(new Catalog()), new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        lastCallback = callback;
      }
    });

    flow.goTo(new Detail());
    lastCallback.onTraversalCompleted();
    try {
      lastCallback.onTraversalCompleted();
    } catch (IllegalStateException e) {
      return;
    }
    Assertions.fail("Second call to onComplete() should have thrown.");
  }

  static class Catalog extends TestScreen {
    Catalog() { super("home"); }
  }

  static class Detail extends TestScreen {
    Detail() { super("detail"); }
  }

  static class Loading extends TestScreen {
    Loading() { super("loading"); }
  }

  static class Error extends TestScreen {
    Error() { super("error"); }
  }

  private void verifyBackstack(Backstack backstack, Object... screens) {
    List<Object> actualScreens = new ArrayList<Object>(backstack.size());
    for (Backstack.Entry entry : backstack) {
      actualScreens.add(entry.getScreen());
    }
    assertThat(actualScreens).containsExactly(screens);
  }
}

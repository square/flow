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

import static org.fest.assertions.api.Assertions.assertThat;

public class ReentranceTest {

  Flow flow;
  Backstack lastStack;
  Flow.Callback lastCallback;

  @Test public void reentrantGo() {
    Flow.Listener listener = new Flow.Listener() {
      @Override public void go(Backstack nextStack, Flow.Direction dir, Flow.Callback callback) {
        lastStack = nextStack;
        Object next = nextStack.current().getScreen();
        if (next instanceof Detail) {
          flow.goTo(new Loading());
        } else if (next instanceof Loading) {
          flow.goTo(new Error());
        }
        callback.onComplete();
      }
    };
    flow = new Flow(Backstack.single(new Catalog()), listener);
    flow.goTo(new Detail());
    verifyBackstack(lastStack, new Error(), new Loading(), new Detail(), new Catalog());
  }

  @Test public void reentrantGoThenBack() {
    Flow.Listener listener = new Flow.Listener() {
      boolean loading = true;
      @Override public void go(Backstack nextStack, Flow.Direction dir, Flow.Callback onComplete) {
        lastStack = nextStack;
        Object next = nextStack.current().getScreen();
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
    flow = new Flow(Backstack.single(new Catalog()), listener);
    flow.goTo(new Detail());
    verifyBackstack(lastStack, new Detail(), new Catalog());
  }

  @Test public void reentrantForwardThenGo() {
    Flow flow = new Flow(Backstack.single(new Catalog()), new Flow.Listener() {
      @Override public void go(Backstack nextStack, Flow.Direction dir, Flow.Callback callback) {
        lastStack = nextStack;
        Object next = nextStack.current().getScreen();
        if (next instanceof Detail) {
          ReentranceTest.this.flow.forward(Backstack.emptyBuilder()
              .push(new Detail())
              .push(new Loading())
              .build());
        } else if (next instanceof Loading) {
          ReentranceTest.this.flow.goTo(new Error());
        }
        callback.onComplete();
      }
    });
    this.flow = flow;
    flow.goTo(new Detail());
    verifyBackstack(lastStack, new Error(), new Loading(), new Detail());
  }

  @Test public void reentranceWaitsForCallback() {
    Flow.Listener listener = new Flow.Listener() {
      @Override public void go(Backstack nextStack, Flow.Direction dir, Flow.Callback callback) {
        lastStack = nextStack;
        lastCallback = callback;
        Object next = nextStack.current().getScreen();
        if (next instanceof Detail) {
          flow.goTo(new Loading());
        } else if (next instanceof Loading) {
          flow.goTo(new Error());
        }
      }
    };
    flow = new Flow(Backstack.single(new Catalog()), listener);
    flow.goTo(new Detail());
    verifyBackstack(flow.getBackstack(), new Catalog());
    lastCallback.onComplete();
    verifyBackstack(flow.getBackstack(), new Detail(), new Catalog());
    lastCallback.onComplete();
    verifyBackstack(flow.getBackstack(), new Loading(), new Detail(), new Catalog());
    lastCallback.onComplete();
    verifyBackstack(flow.getBackstack(), new Error(), new Loading(), new Detail(), new Catalog());
  }

  @Test public void onCompleteThrowsIfCalledTwice() {
    flow = new Flow(Backstack.single(new Catalog()), new Flow.Listener() {
      @Override public void go(Backstack nextBackstack, Flow.Direction direction,
          Flow.Callback callback) {
        lastStack = nextBackstack;
        lastCallback = callback;
      }
    });

    flow.goTo(new Detail());
    lastCallback.onComplete();
    try {
      lastCallback.onComplete();
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

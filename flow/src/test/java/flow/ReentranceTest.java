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
import java.util.concurrent.atomic.AtomicInteger;
import org.fest.assertions.api.Assertions;
import org.junit.Test;

import static flow.Flow.Direction.FORWARD;
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
        Object next = navigation.destination.current();
        if (next instanceof Detail) {
          flow.set(new Loading());
        } else if (next instanceof Loading) {
          flow.set(new Error());
        }
        callback.onTraversalCompleted();
      }
    };
    flow = new Flow(Backstack.single(new Catalog()));
    flow.setDispatcher(dispatcher);
    flow.set(new Detail());
    verifyBackstack(lastStack, new Error(), new Loading(), new Detail(), new Catalog());
  }

  @Test public void reentrantGoThenBack() {
    Flow.Dispatcher dispatcher = new Flow.Dispatcher() {
      boolean loading = true;

      @Override public void dispatch(Traversal navigation, TraversalCallback onComplete) {
        lastStack = navigation.destination;
        Object next = navigation.destination.current();
        if (loading) {
          if (next instanceof Detail) {
            flow.set(new Loading());
          } else if (next instanceof Loading) {
            flow.set(new Error());
          } else if (next instanceof Error) {
            loading = false;
            flow.goBack();
          }
        } else {
          if (next instanceof Loading) {
            ReentranceTest.this.flow.goBack();
          }
        }
        onComplete.onTraversalCompleted();
      }
    };
    flow = new Flow(Backstack.single(new Catalog()));
    flow.setDispatcher(dispatcher);
    flow.set(new Detail());
    verifyBackstack(lastStack, new Detail(), new Catalog());
  }

  @Test public void reentrantForwardThenGo() {
    Flow flow = new Flow(Backstack.single(new Catalog()));
    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        Object next = traversal.destination.current();
        if (next instanceof Detail) {
          ReentranceTest.this.flow.setBackstack(
              Backstack.emptyBuilder().push(new Detail()).push(new Loading()).build(), FORWARD);
        } else if (next instanceof Loading) {
          ReentranceTest.this.flow.set(new Error());
        }
        callback.onTraversalCompleted();
      }
    });
    this.flow = flow;
    flow.set(new Detail());
    verifyBackstack(lastStack, new Error(), new Loading(), new Detail());
  }

  @Test public void reentranceWaitsForCallback() {
    Flow.Dispatcher dispatcher = new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        lastCallback = callback;
        Object next = traversal.destination.current();
        if (next instanceof Detail) {
          flow.set(new Loading());
        } else if (next instanceof Loading) {
          flow.set(new Error());
        }
      }
    };
    flow = new Flow(Backstack.single(new Catalog()));
    flow.setDispatcher(dispatcher);
    lastCallback.onTraversalCompleted();

    flow.set(new Detail());
    verifyBackstack(flow.getBackstack(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyBackstack(flow.getBackstack(), new Detail(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyBackstack(flow.getBackstack(), new Loading(), new Detail(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyBackstack(flow.getBackstack(), new Error(), new Loading(), new Detail(), new Catalog());
  }

  @Test public void onCompleteThrowsIfCalledTwice() {
    flow = new Flow(Backstack.single(new Catalog()));
    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        lastCallback = callback;
      }
    });

    lastCallback.onTraversalCompleted();
    try {
      lastCallback.onTraversalCompleted();
    } catch (IllegalStateException e) {
      return;
    }
    Assertions.fail("Second call to onComplete() should have thrown.");
  }

  @Test public void bootstrapTraversal() {
    flow = new Flow(Backstack.single(new Catalog()));

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        callback.onTraversalCompleted();
      }
    });

    verifyBackstack(lastStack, new Catalog());
  }

  @Test public void pendingTraversalReplacesBootstrap() {
    final AtomicInteger dispatchCount = new AtomicInteger(0);
    flow = new Flow(Backstack.single(new Catalog()));
    flow.set(new Detail());

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        dispatchCount.incrementAndGet();
        lastStack = traversal.destination;
        callback.onTraversalCompleted();
      }
    });

    verifyBackstack(lastStack, new Detail(), new Catalog());
    assertThat(dispatchCount.intValue()).isEqualTo(1);
  }

  @Test public void allPendingTraversalsFire() {
    flow = new Flow(Backstack.single(new Catalog()));
    flow.set(new Loading());
    flow.set(new Detail());
    flow.set(new Error());

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastCallback = callback;
      }
    });

    lastCallback.onTraversalCompleted();
    verifyBackstack(flow.getBackstack(), new Loading(), new Catalog());

    lastCallback.onTraversalCompleted();
    verifyBackstack(flow.getBackstack(), new Detail(), new Loading(), new Catalog());
  }

  @Test public void clearingDispatcherMidTraversalPauses() {
    flow = new Flow(Backstack.single(new Catalog()));

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        flow.set(new Loading());
        flow.removeDispatcher(this);
        callback.onTraversalCompleted();
      }
    });

    verifyBackstack(flow.getBackstack(), new Catalog());

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        callback.onTraversalCompleted();
      }
    });

    verifyBackstack(flow.getBackstack(), new Loading(), new Catalog());
  }

  @Test public void dispatcherSetInMidFlightWaitsForBootstrap() {
    flow = new Flow(Backstack.single(new Catalog()));
    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastCallback = callback;
      }
    });
    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        callback.onTraversalCompleted();
      }
    });

    assertThat(lastStack).isNull();
    lastCallback.onTraversalCompleted();
    verifyBackstack(lastStack, new Catalog());
  }

  @Test public void dispatcherSetInMidFlightWithBigQueueNeedsNoBootstrap() {
    final AtomicInteger secondDispatcherCount = new AtomicInteger(0);
    flow = new Flow(Backstack.single(new Catalog()));
    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        flow.set(new Detail());
        lastCallback = callback;
      }
    });
    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        secondDispatcherCount.incrementAndGet();
        lastStack = traversal.destination;
        callback.onTraversalCompleted();
      }
    });

    assertThat(lastStack).isNull();
    lastCallback.onTraversalCompleted();
    verifyBackstack(lastStack, new Detail(), new Catalog());
    assertThat(secondDispatcherCount.get()).isEqualTo(1);
  }

  @Test public void traversalsQueuedAfterDispatcherRemovedBootstrapTheNextOne() {
    final AtomicInteger secondDispatcherCount = new AtomicInteger(0);
    flow = new Flow(Backstack.single(new Catalog()));

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastCallback = callback;
        flow.removeDispatcher(this);
        flow.set(new Loading());
      }
    });

    verifyBackstack(flow.getBackstack(), new Catalog());

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        secondDispatcherCount.incrementAndGet();
        callback.onTraversalCompleted();
      }
    });

    assertThat(secondDispatcherCount.get()).isZero();
    lastCallback.onTraversalCompleted();

    assertThat(secondDispatcherCount.get()).isEqualTo(1);
    verifyBackstack(flow.getBackstack(), new Loading(), new Catalog());
  }

  static class Catalog extends TestPath {
    Catalog() {
      super("catalog");
    }
  }

  static class Detail extends TestPath {
    Detail() {
      super("detail");
    }
  }

  static class Loading extends TestPath {
    Loading() {
      super("loading");
    }
  }

  static class Error extends TestPath {
    Error() {
      super("error");
    }
  }

  private void verifyBackstack(Backstack backstack, Object... screens) {
    List<Object> actualScreens = new ArrayList<>(backstack.size());
    for (Path entry : backstack) {
      actualScreens.add(entry);
    }
    assertThat(actualScreens).containsExactly(screens);
  }
}

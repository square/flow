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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static flow.Flow.Direction.FORWARD;
import static flow.Flow.Traversal;
import static flow.Flow.TraversalCallback;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.MockitoAnnotations.initMocks;

public class ReentranceTest {

  @Mock KeyManager keyManager;
  Flow flow;
  History lastStack;
  TraversalCallback lastCallback;

  @Before public void setUp() {
    initMocks(this);
  }

  @Test public void reentrantGo() {
    Flow.Dispatcher dispatcher = new Flow.Dispatcher() {
      @Override public void dispatch(Traversal navigation, TraversalCallback callback) {
        lastStack = navigation.destination;
        Object next = navigation.destination.top();
        if (next instanceof Detail) {
          flow.set(new Loading());
        } else if (next instanceof Loading) {
          flow.set(new Error());
        }
        callback.onTraversalCompleted();
      }
    };
    flow = new Flow(keyManager, History.single(new Catalog()));
    flow.setDispatcher(dispatcher);
    flow.set(new Detail());
    verifyHistory(lastStack, new Error(), new Loading(), new Detail(), new Catalog());
  }

  @Test public void reentrantGoThenBack() {
    Flow.Dispatcher dispatcher = new Flow.Dispatcher() {
      boolean loading = true;

      @Override public void dispatch(Traversal navigation, TraversalCallback onComplete) {
        lastStack = navigation.destination;
        Object next = navigation.destination.top();
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
    flow = new Flow(keyManager, History.single(new Catalog()));
    flow.setDispatcher(dispatcher);
    flow.set(new Detail());
    verifyHistory(lastStack, new Detail(), new Catalog());
  }

  @Test public void reentrantForwardThenGo() {
    Flow flow = new Flow(keyManager, History.single(new Catalog()));
    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        Object next = traversal.destination.top();
        if (next instanceof Detail) {
          ReentranceTest.this.flow.setHistory(
              History.emptyBuilder().push(new Detail()).push(new Loading()).build(), FORWARD);
        } else if (next instanceof Loading) {
          ReentranceTest.this.flow.set(new Error());
        }
        callback.onTraversalCompleted();
      }
    });
    this.flow = flow;
    flow.set(new Detail());
    verifyHistory(lastStack, new Error(), new Loading(), new Detail());
  }

  @Test public void reentranceWaitsForCallback() {
    Flow.Dispatcher dispatcher = new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        lastCallback = callback;
        Object next = traversal.destination.top();
        if (next instanceof Detail) {
          flow.set(new Loading());
        } else if (next instanceof Loading) {
          flow.set(new Error());
        }
      }
    };
    flow = new Flow(keyManager, History.single(new Catalog()));
    flow.setDispatcher(dispatcher);
    lastCallback.onTraversalCompleted();

    flow.set(new Detail());
    verifyHistory(flow.getHistory(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyHistory(flow.getHistory(), new Detail(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyHistory(flow.getHistory(), new Loading(), new Detail(), new Catalog());
    lastCallback.onTraversalCompleted();
    verifyHistory(flow.getHistory(), new Error(), new Loading(), new Detail(), new Catalog());
  }

  @Test public void onCompleteThrowsIfCalledTwice() {
    flow = new Flow(keyManager, History.single(new Catalog()));
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
    fail("Second call to onComplete() should have thrown.");
  }

  @Test public void bootstrapTraversal() {
    flow = new Flow(keyManager, History.single(new Catalog()));

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastStack = traversal.destination;
        callback.onTraversalCompleted();
      }
    });

    verifyHistory(lastStack, new Catalog());
  }

  @Test public void pendingTraversalReplacesBootstrap() {
    final AtomicInteger dispatchCount = new AtomicInteger(0);
    flow = new Flow(keyManager, History.single(new Catalog()));
    flow.set(new Detail());

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        dispatchCount.incrementAndGet();
        lastStack = traversal.destination;
        callback.onTraversalCompleted();
      }
    });

    verifyHistory(lastStack, new Detail(), new Catalog());
    assertThat(dispatchCount.intValue()).isEqualTo(1);
  }

  @Test public void allPendingTraversalsFire() {
    flow = new Flow(keyManager, History.single(new Catalog()));
    flow.set(new Loading());
    flow.set(new Detail());
    flow.set(new Error());

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastCallback = callback;
      }
    });

    lastCallback.onTraversalCompleted();
    verifyHistory(flow.getHistory(), new Loading(), new Catalog());

    lastCallback.onTraversalCompleted();
    verifyHistory(flow.getHistory(), new Detail(), new Loading(), new Catalog());
  }

  @Test public void clearingDispatcherMidTraversalPauses() {
    flow = new Flow(keyManager, History.single(new Catalog()));

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        flow.set(new Loading());
        flow.removeDispatcher(this);
        callback.onTraversalCompleted();
      }
    });

    verifyHistory(flow.getHistory(), new Catalog());

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        callback.onTraversalCompleted();
      }
    });

    verifyHistory(flow.getHistory(), new Loading(), new Catalog());
  }

  @Test public void dispatcherSetInMidFlightWaitsForBootstrap() {
    flow = new Flow(keyManager, History.single(new Catalog()));
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
    verifyHistory(lastStack, new Catalog());
  }

  @Test public void dispatcherSetInMidFlightWithBigQueueNeedsNoBootstrap() {
    final AtomicInteger secondDispatcherCount = new AtomicInteger(0);
    flow = new Flow(keyManager, History.single(new Catalog()));
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
    verifyHistory(lastStack, new Detail(), new Catalog());
    assertThat(secondDispatcherCount.get()).isEqualTo(1);
  }

  @Test public void traversalsQueuedAfterDispatcherRemovedBootstrapTheNextOne() {
    final AtomicInteger secondDispatcherCount = new AtomicInteger(0);
    flow = new Flow(keyManager, History.single(new Catalog()));

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        lastCallback = callback;
        flow.removeDispatcher(this);
        flow.set(new Loading());
      }
    });

    verifyHistory(flow.getHistory(), new Catalog());

    flow.setDispatcher(new Flow.Dispatcher() {
      @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
        secondDispatcherCount.incrementAndGet();
        callback.onTraversalCompleted();
      }
    });

    assertThat(secondDispatcherCount.get()).isZero();
    lastCallback.onTraversalCompleted();

    assertThat(secondDispatcherCount.get()).isEqualTo(1);
    verifyHistory(flow.getHistory(), new Loading(), new Catalog());
  }

  static class Catalog extends TestState {
    Catalog() {
      super("catalog");
    }
  }

  static class Detail extends TestState {
    Detail() {
      super("detail");
    }
  }

  static class Loading extends TestState {
    Loading() {
      super("loading");
    }
  }

  static class Error extends TestState {
    Error() {
      super("error");
    }
  }

  private void verifyHistory(History history, Object... screens) {
    List<Object> actualScreens = new ArrayList<>(history.size());
    for (Object entry : history) {
      actualScreens.add(entry);
    }
    assertThat(actualScreens).containsExactly(screens);
  }
}

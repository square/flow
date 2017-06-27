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

import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static flow.Direction.FORWARD;
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
    Dispatcher dispatcher = new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal navigation, @NonNull TraversalCallback callback) {
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
    Dispatcher dispatcher = new Dispatcher() {
      boolean loading = true;

      @Override
      public void dispatch(@NonNull Traversal navigation, @NonNull TraversalCallback onComplete) {
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
    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
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

  @Test public void goBackQueuesUp() {
    final Queue<TraversalCallback> callbacks = new LinkedList<>();

    Flow flow = new Flow(keyManager, History.single(new Catalog()));
    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
        callbacks.add(callback);
        lastStack = traversal.destination;
      }
    });

    flow.set(new Detail());
    flow.set(new Error());
    assertThat(flow.goBack()).isTrue();

    while (!callbacks.isEmpty()) {
      callbacks.poll().onTraversalCompleted();
    }

    verifyHistory(lastStack, new Detail(), new Catalog());
  }

  @Test public void overwflowQueuedBackupsNoOp() {
    final Queue<TraversalCallback> callbacks = new LinkedList<>();

    Flow flow = new Flow(keyManager, History.single(new Catalog()));
    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
        callbacks.add(callback);
        lastStack = traversal.destination;
      }
    });

    flow.set(new Detail());

    for (int i = 0; i < 20; i++) {
      assertThat(flow.goBack()).isTrue();
    }

    int callbackCount = 0;
    while (!callbacks.isEmpty()) {
      callbackCount++;
      callbacks.poll().onTraversalCompleted();
    }

    assertThat(callbackCount).isEqualTo(3);
    verifyHistory(lastStack, new Catalog());
  }

  @Test public void reentranceWaitsForCallback() {
    Dispatcher dispatcher = new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
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
    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
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

    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
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

    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
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

    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
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

    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
        flow.set(new Loading());
        flow.removeDispatcher(this);
        callback.onTraversalCompleted();
      }
    });

    verifyHistory(flow.getHistory(), new Catalog());

    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
        callback.onTraversalCompleted();
      }
    });

    verifyHistory(flow.getHistory(), new Loading(), new Catalog());
  }

  @Test public void dispatcherSetInMidFlightWaitsForBootstrap() {
    flow = new Flow(keyManager, History.single(new Catalog()));
    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
        lastCallback = callback;
      }
    });
    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
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
    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
        flow.set(new Detail());
        lastCallback = callback;
      }
    });
    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
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

    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
        lastCallback = callback;
        flow.removeDispatcher(this);
        flow.set(new Loading());
      }
    });

    verifyHistory(flow.getHistory(), new Catalog());

    flow.setDispatcher(new Dispatcher() {
      @Override
      public void dispatch(@NonNull Traversal traversal, @NonNull TraversalCallback callback) {
        secondDispatcherCount.incrementAndGet();
        callback.onTraversalCompleted();
      }
    });

    assertThat(secondDispatcherCount.get()).isZero();
    lastCallback.onTraversalCompleted();

    assertThat(secondDispatcherCount.get()).isEqualTo(1);
    verifyHistory(flow.getHistory(), new Loading(), new Catalog());
  }

  static class Catalog extends TestKey {
    Catalog() {
      super("catalog");
    }
  }

  static class Detail extends TestKey {
    Detail() {
      super("detail");
    }
  }

  static class Loading extends TestKey {
    Loading() {
      super("loading");
    }
  }

  static class Error extends TestKey {
    Error() {
      super("error");
    }
  }

  private void verifyHistory(History history, Object... keys) {
    List<Object> actualKeys = new ArrayList<>(history.size());
    for (Object entry : history.framesFromTop()) {
      actualKeys.add(entry);
    }
    assertThat(actualKeys).containsExactly(keys);
  }
}

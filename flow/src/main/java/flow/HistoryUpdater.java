package flow;

import java.util.Iterator;

/**
 * A function that calculates the next state of a given {@link History}.
 */
public interface HistoryUpdater {

  Result call(History history);

  final class Result {
    public final History history;
    public final Direction direction;

    public Result(History history, Direction direction) {
      this.history = history;
      this.direction = direction;
    }

    @Override public String toString() {
      return String.format("%s: %s to %s", getClass().getName(), direction, history);
    }
  }

  final class DoSet implements HistoryUpdater {
    private final Object newTopKey;

    public DoSet(Object newTopKey) {
      this.newTopKey = newTopKey;
    }

    @Override public Result call(History history) {
      if (newTopKey.equals(history.top())) {
        return new Result(history, Direction.REPLACE);
      }

      History.Builder builder = history.buildUpon();
      int count = 0;
      // Search backward to see if we already have newTop on the stack
      Object preservedInstance = null;
      for (Iterator<Object> it = history.reverseIterator(); it.hasNext(); ) {
        Object entry = it.next();

        // If we find newTop on the stack, pop back to it.
        if (entry.equals(newTopKey)) {
          for (int i = 0; i < history.size() - count; i++) {
            preservedInstance = builder.pop();
          }
          break;
        } else {
          count++;
        }
      }

      History newHistory;
      if (preservedInstance != null) {
        // newTop was on the history. Put the preserved instance back on and dispatch.
        builder.push(preservedInstance);
        newHistory = builder.build();
        return new Result(newHistory, Direction.BACKWARD);
      }
      // newTop was not on the history. Push it on and dispatch.
      builder.push(newTopKey);
      newHistory = builder.build();
      return new Result(newHistory, Direction.FORWARD);
    }
  }
}

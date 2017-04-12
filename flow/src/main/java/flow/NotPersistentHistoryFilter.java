package flow;

import android.support.annotation.NonNull;
import java.util.Iterator;

/**
 * Default implementation of {@link HistoryFilter}, enforces the contract
 * documented on {@link NotPersistent}.
 */
class NotPersistentHistoryFilter implements HistoryFilter {
  @NonNull @Override public History scrubHistory(@NonNull History history) {
    History.Builder builder = History.emptyBuilder();

    final Iterator<Object> keys = history.reverseIterator();
    while (keys.hasNext()) {
      Object key = keys.next();
      if (!key.getClass().isAnnotationPresent(NotPersistent.class)) {
        builder.push(key);
      }
    }

    return builder.build();
  }
}

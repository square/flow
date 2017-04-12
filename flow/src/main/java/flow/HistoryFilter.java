package flow;

import android.support.annotation.NonNull;

/**
 * An object to which gets a chance to scrub the current {@link History} before
 * it is persisted.
 */
public interface HistoryFilter {
  @NonNull History scrubHistory(@NonNull History history);
}

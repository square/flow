package flow.sample.basic;

import android.os.Parcelable;
import flow.StateParceler;

/**
 * Assumes states are {@link Parcelable}.
 *
 * A more realistic implementation might rely on a library like auto-value-parcel,
 * auto-parcel, or parceler.
 * */
final class BasicStateParceler implements StateParceler {
  @Override public Parcelable toParcelable(Object state) {
    return (Parcelable) state;
  }

  @Override public Object toState(Parcelable parcelable) {
    return parcelable;
  }
}

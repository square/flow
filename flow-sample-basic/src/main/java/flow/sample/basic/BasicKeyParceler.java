package flow.sample.basic;

import android.os.Parcelable;
import flow.KeyParceler;

/**
 * Assumes states are {@link Parcelable}.
 *
 * A more realistic implementation might rely on a library like auto-value-parcel,
 * auto-parcel, or parceler.
 * */
final class BasicKeyParceler implements KeyParceler {
  @Override public Parcelable toParcelable(Object key) {
    return (Parcelable) key;
  }

  @Override public Object toKey(Parcelable parcelable) {
    return parcelable;
  }
}

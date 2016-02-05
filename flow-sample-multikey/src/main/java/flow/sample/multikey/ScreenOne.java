package flow.sample.multikey;

public final class ScreenOne {
  @Override public String toString() {
    return "Click to advance to screen two.";
  }

  @Override public boolean equals(Object o) {
    return getClass().isInstance(o);
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}

package flow.sample.multikey;

public class ScreenTwo {
  @Override public String toString() {
    return "Click to pop back to screen one, skipping the dialog. "
        + "Or hit the back button to see the dialog again.";
  }

  @Override public boolean equals(Object o) {
    return getClass().isInstance(o);
  }

  @Override public int hashCode() {
    return getClass().hashCode();
  }
}

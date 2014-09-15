package flow;

class TestScreen {
  final String name;

  TestScreen(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TestScreen screen = (TestScreen) o;
    return name.equals(screen.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override public String toString() {
    return String.format("%s{%h}", name, this);
  }
}

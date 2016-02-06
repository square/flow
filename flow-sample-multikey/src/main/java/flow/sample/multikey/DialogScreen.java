package flow.sample.multikey;

import flow.MultiKey;
import java.util.Collections;
import java.util.List;

public final class DialogScreen implements MultiKey {
  final Object mainContent;

  public DialogScreen(Object mainContent) {
    this.mainContent = mainContent;
  }

  @Override public String toString() {
    return "Do you really want to see screen two?";
  }

  @Override public List<Object> getKeys() {
    return Collections.singletonList(mainContent);
  }
}

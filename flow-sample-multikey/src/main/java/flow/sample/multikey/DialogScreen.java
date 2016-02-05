package flow.sample.multikey;

import flow.MultiKey;
import java.util.Arrays;
import java.util.List;

public final class DialogScreen implements MultiKey {
  final Object mainContent;
  final Object dialogContent;

  public DialogScreen(Object mainContent, Object dialogContent) {
    this.mainContent = mainContent;
    this.dialogContent = dialogContent;
  }

  @Override public List<Object> getKeys() {
    return Arrays.asList(mainContent, dialogContent);
  }
}

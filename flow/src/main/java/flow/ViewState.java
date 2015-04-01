package flow;

import android.view.View;

public interface ViewState {
  public void save(View view);
  public void restore(View view);
}

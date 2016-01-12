package flow.sample.basic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import flow.Flow;

public final class WelcomeView extends LinearLayout {

  public WelcomeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    EditText nameView = (EditText) findViewById(R.id.welcome_screen_name);

    nameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        Flow.get(view).set(new HelloScreen(view.getText().toString()));
        return true;
      }
    });
  }
}

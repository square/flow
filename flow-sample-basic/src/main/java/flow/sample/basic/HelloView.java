package flow.sample.basic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class HelloView extends LinearLayout {

  public HelloView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    HelloScreen screen = Screens.getScreen(getContext());
    ((TextView) findViewById(R.id.hello_name)).setText("Hello " + screen.name);

    final TextView counter = (TextView) findViewById(R.id.hello_counter);
    findViewById(R.id.hello_increment).setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        String text = counter.getText().toString();
        if (text.isEmpty()) text = "0";
        Integer current = Integer.valueOf(text);
        counter.setText(String.valueOf(current + 1));
      }
    });
  }
}

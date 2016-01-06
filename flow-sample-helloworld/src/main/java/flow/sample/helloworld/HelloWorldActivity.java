package flow.sample.helloworld;

import android.app.Activity;
import android.content.Context;
import flow.Flow;

public class HelloWorldActivity extends Activity {

  @Override protected void attachBaseContext(Context baseContext) {
    baseContext = Flow.configure(baseContext, this).install();
    super.attachBaseContext(baseContext);
  }

  @Override public void onBackPressed() {
    if (!Flow.onBackPressed(this)) {
      super.onBackPressed();
    }
  }
}

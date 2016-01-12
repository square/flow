package flow.sample.basic;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import flow.Flow;

public class BasicSampleActivity extends Activity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.basic_activity_frame);
  }

  @Override protected void attachBaseContext(Context baseContext) {
    baseContext = Flow.configure(baseContext, this) //
        .dispatcher(new BasicDispatcher(this)) //
        .defaultState(new WelcomeScreen()) //
        .stateParceler(new BasicStateParceler()) //
        .install();
    super.attachBaseContext(baseContext);
  }

  @Override public void onBackPressed() {
    if (!Flow.onBackPressed(this)) {
      super.onBackPressed();
    }
  }
}

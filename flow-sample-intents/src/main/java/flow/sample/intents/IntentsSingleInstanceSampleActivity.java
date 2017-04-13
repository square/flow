/*
 * Copyright 2017 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package flow.sample.intents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import flow.Flow;

public class IntentsSingleInstanceSampleActivity extends Activity {

  @Override protected void attachBaseContext(Context baseContext) {
    baseContext = Flow.configure(baseContext, this).keyParceler(new StringParceler()).install();
    super.attachBaseContext(baseContext);
  }

  @Override protected void onNewIntent(Intent intent) {
    Flow.onNewIntent(intent, this);
  }

  @Override public void onBackPressed() {
    if (!Flow.get(this).goBack()) {
      super.onBackPressed();
    }
  }
}

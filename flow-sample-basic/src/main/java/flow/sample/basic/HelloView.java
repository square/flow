/*
 * Copyright 2016 Square Inc.
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

package flow.sample.basic;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import flow.Flow;

public final class HelloView extends LinearLayout {

  public HelloView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    HelloScreen screen = Flow.getKey(this);
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

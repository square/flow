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

package flow.sample.orientation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import flow.Flow;

public final class LandscapeView extends LinearLayout {

  public LandscapeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    findViewById(R.id.landscape_text).setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        Flow.get(getContext()).set(LooseScreen.INSTANCE);
      }
    });
  }
}

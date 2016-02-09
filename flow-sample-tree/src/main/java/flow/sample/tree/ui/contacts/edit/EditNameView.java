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

package flow.sample.tree.ui.contacts.edit;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import flow.Flow;
import flow.sample.tree.FlowServices;
import flow.sample.tree.R;
import flow.sample.tree.model.ContactEditor;

public class EditNameView extends LinearLayout {

  private ContactEditor editor;
  private TextView emailView;
  private EditText nameView;
  private TextWatcher nameWatcher;

  public EditNameView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    editor = Flow.getService(FlowServices.CONTACT_EDITOR, getContext());
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    emailView = (TextView) findViewById(R.id.email);
    nameView = (EditText) findViewById(R.id.edit_name);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    emailView.setText(editor.email);
    nameView.setText(editor.name);
    nameWatcher = new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        editor.name = s.toString();
      }

      @Override public void afterTextChanged(Editable s) {
      }
    };
    nameView.addTextChangedListener(nameWatcher);

    nameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        Flow.get(v).set(new EditEmailScreen(editor.id));
        return true;
      }
    });
  }

  @Override protected void onDetachedFromWindow() {
    nameView.setOnEditorActionListener(null);
    nameView.removeTextChangedListener(nameWatcher);
    super.onDetachedFromWindow();
  }
}

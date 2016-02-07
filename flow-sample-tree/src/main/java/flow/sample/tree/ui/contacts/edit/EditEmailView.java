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
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import flow.Flow;
import flow.sample.tree.FlowServices;
import flow.sample.tree.R;
import flow.sample.tree.model.ContactEditor;
import flow.sample.tree.model.ContactsStorage;
import flow.sample.tree.ui.contacts.list.ListContactsScreen;

public class EditEmailView extends LinearLayout {

  ContactEditor editor;
  private TextView nameView;
  private EditText emailView;
  private TextWatcher emailWatcher;
  private View saveButton;

  public EditEmailView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    editor = Flow.getService(FlowServices.CONTACT_EDITOR, context);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    nameView = (TextView) findViewById(R.id.name);
    emailView = (EditText) findViewById(R.id.edit_email);
    saveButton = findViewById(R.id.save);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    nameView.setText(editor.name);
    emailView.setText(editor.email);
    emailWatcher = new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        editor.email = s.toString();
      }

      @Override public void afterTextChanged(Editable s) {
      }
    };
    emailView.addTextChangedListener(emailWatcher);

    saveButton.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        editor.email = emailView.getText().toString();
        ContactsStorage storage = Flow.getService(FlowServices.CONTACTS_STORAGE, v.getContext());
        //noinspection ConstantConditions
        storage.save(editor.toContact());
        Flow.get(v).set(new ListContactsScreen());
      }
    });
  }

  @Override protected void onDetachedFromWindow() {
    saveButton.setOnClickListener(null);
    emailView.removeTextChangedListener(emailWatcher);
    super.onDetachedFromWindow();
  }
}

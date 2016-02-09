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

package flow.sample.tree.ui.contacts.list;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import flow.Flow;
import flow.sample.tree.FlowServices;
import flow.sample.tree.model.Contact;
import flow.sample.tree.model.ContactsStorage;
import flow.sample.tree.ui.contacts.edit.EditNameScreen;

public class ListContactsView extends ListView {

  private final ContactsAdapter adapter = new ContactsAdapter();

  private ContactsStorage storage;

  public ListContactsView(Context context, AttributeSet attrs) {
    super(context, attrs);
    storage = Flow.getService(FlowServices.CONTACTS_STORAGE, context);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    setAdapter(adapter);
    setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Contact contact = adapter.getItem(position);
        Flow.get(ListContactsView.this).set(new EditNameScreen(contact.id));
      }
    });
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    adapter.setContacts(storage.listContacts());
  }
}

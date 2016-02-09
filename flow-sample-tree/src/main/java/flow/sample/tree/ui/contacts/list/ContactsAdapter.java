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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import flow.sample.tree.R;
import flow.sample.tree.model.Contact;
import java.util.ArrayList;
import java.util.List;

final class ContactsAdapter extends BaseAdapter {
  private final List<Contact> contacts = new ArrayList<>();

  public void setContacts(List<Contact> contacts) {
    this.contacts.clear();
    this.contacts.addAll(contacts);
    notifyDataSetChanged();
  }

  @Override public int getCount() {
    return contacts.size();
  }

  @Override public Contact getItem(int position) {
    return contacts.get(position);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View convertView, ViewGroup parent) {
    Contact contact = getItem(position);
    View view = convertView;
    if (view == null) {
      final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      view = inflater.inflate(R.layout.list_contacts_screen_row_view, parent, false);
    }
    ((TextView) view.findViewById(R.id.contact_name)).setText(contact.name);
    ((TextView) view.findViewById(R.id.contact_email)).setText(contact.email);
    return view;
  }
}

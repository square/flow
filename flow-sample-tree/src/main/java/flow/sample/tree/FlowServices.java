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

package flow.sample.tree;

import android.support.annotation.NonNull;
import flow.Services;
import flow.ServicesFactory;
import flow.sample.tree.model.Contact;
import flow.sample.tree.model.ContactEditor;
import flow.sample.tree.model.ContactsStorage;
import flow.sample.tree.ui.contacts.ContactsUiKey;
import flow.sample.tree.ui.contacts.edit.EditContactKey;

public final class FlowServices extends ServicesFactory {
  public static final String CONTACTS_STORAGE = "CONTACTS_STORAGE";
  public static final String CONTACT_EDITOR = "CONTACT_EDITOR";

  // In a real app, the conditional class matching shown here doesn't scale very far. Decompose by
  // keys. Even better, keep your ServicesFactory lean and simple by using the key to build/lookup
  // a Dagger graph or Mortar scope!

  @Override public void bindServices(@NonNull Services.Binder services) {
    Object key = services.getKey();
    if (key.equals(new ContactsUiKey())) {
      // Setting up the ContactsUiKey means providing storage for Contacts.
      services.bind(CONTACTS_STORAGE, new ContactsStorage());
    } else if (key instanceof EditContactKey) {
      // Setting up the EditContactKey key means providing an editor for the contact.
      // This editor can be shared among any keys that have the EditContactKey as parent/ancestor!
      final String contactId = ((EditContactKey) key).contactId;
      ContactsStorage storage = services.getService(CONTACTS_STORAGE);
      Contact contact = storage.getContact(contactId);
      services.bind(CONTACT_EDITOR, new ContactEditor(contact));
    }
  }

  @Override public void tearDownServices(Services services) {
    // Nothing to do in this example, but if you need this hook to release resources, it's here!
    super.tearDownServices(services);
  }
}

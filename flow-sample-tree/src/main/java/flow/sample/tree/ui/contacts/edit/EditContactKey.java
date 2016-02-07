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

import flow.TreeKey;
import flow.sample.tree.ui.contacts.ContactsUiKey;

public final class EditContactKey extends ContactKey implements TreeKey {

  EditContactKey(String contactId) {
    super(contactId);
  }

  @Override public Object getParentKey() {
    return new ContactsUiKey();
  }
}

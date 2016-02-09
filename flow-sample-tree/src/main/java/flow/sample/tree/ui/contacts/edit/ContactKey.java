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

import flow.ClassKey;

abstract class ContactKey extends ClassKey {
  public final String contactId;

  protected ContactKey(String contactId) {
    this.contactId = contactId;
  }

  @Override public boolean equals(Object o) {
    return super.equals(o) && contactId.equals(((ContactKey) o).contactId);
  }

  @Override public int hashCode() {
    return contactId.hashCode();
  }

  @Override public String toString() {
    return getClass().getSimpleName() + "{contactId=" + contactId + "}";
  }
}

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

package flow.sample.tree.model;

public final class ContactEditor {
  public final String id;
  public String name;
  public String email;

  public ContactEditor(Contact contact) {
    this.id = contact.id;
    this.name = contact.name;
    this.email = contact.email;
  }

  public Contact toContact() {
    return new Contact(id, name, email);
  }
}

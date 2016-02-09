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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

public final class ContactsStorage {
  public static final Contact HOMER =
      new Contact("iuoxcuioxcuiov", "Homer Simpson", "homer@example.net");
  public static final Contact MARGE =
      new Contact("9sduoihoi9h0h9", "Marge Simpson", "marge@example.net");
  public static final Contact BART =
      new Contact("kjaskdjaksdjad", "Bart Simpson", "bart@example.net");
  public static final Contact LISA =
      new Contact("n9chzdfofnoifh", "Lisa Simpson", "lisa@example.net");
  public static final Contact MAGGIE =
      new Contact("a1j3j4k1241jk2", "Maggie Simpson", "maggie@example.net");

  private final Map<String, Contact> contacts = new LinkedHashMap<>();

  public ContactsStorage() {
    save(HOMER);
    save(MARGE);
    save(BART);
    save(LISA);
    save(MAGGIE);
  }

  public List<Contact> listContacts() {
    return unmodifiableList(new ArrayList<>(contacts.values()));
  }

  public Contact getContact(String id) {
    return contacts.get(id);
  }

  public void save(Contact contact) {
    contacts.put(contact.id, contact);
  }
}

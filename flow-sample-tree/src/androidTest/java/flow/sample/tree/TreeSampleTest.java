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

import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import flow.sample.tree.model.Contact;
import flow.sample.tree.ui.welcome.WelcomeScreen;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static flow.sample.tree.FlowServices.CONTACTS_STORAGE;
import static flow.sample.tree.FlowServices.CONTACT_EDITOR;
import static flow.sample.tree.FlowViewAssertions.doesNotHaveFlowService;
import static flow.sample.tree.FlowViewAssertions.hasFlowService;
import static flow.sample.tree.model.ContactsStorage.HOMER;

public class TreeSampleTest {

  @Rule public ActivityTestRule rule = new ActivityTestRule<>(TreeSampleActivity.class);

  @Test public void editsAreSaved() {
    // We start on the welcome screen.

    // WelcomeScreen does not have ContactsUiKey as an ancestor, so it shouldn't have contacts.
    onView(withParent(withId(android.R.id.content))) //
        .check(doesNotHaveFlowService(CONTACTS_STORAGE)) //
        .check(doesNotHaveFlowService(CONTACT_EDITOR));

    // Click through.
    onView(withText(new WelcomeScreen().toString())).perform(click());

    // We should now be on the ListContactsScreen, which has ContactsUiKey as its parent.
    // Since we've entered ContactsUiKey, ContactsStorage should be initialized and available.
    onView(withParent(withId(android.R.id.content))) //
        .check(hasFlowService(CONTACTS_STORAGE)) //
        // We shouldn't have a contact editor yet though
        .check(doesNotHaveFlowService(CONTACT_EDITOR));

    // We should be looking at our list of contacts. Click on Homer.
    onData(contactWithId(HOMER.id)).perform(click());

    // Now we should be on the EditNameScreen
    onView(withParent(withId(android.R.id.content))) //
        // Since EditNameScreen is an EditContactKey, it should have a contact editor.
        .check(hasFlowService(CONTACT_EDITOR)) //
        // Its parent is ContactsUiKey, so we should still have the storage service, too.
        .check(hasFlowService(CONTACTS_STORAGE));

    String originalName = HOMER.name;
    String originalEmail = HOMER.email;
    String editedName = originalName + " Edited";
    String editedEmail = originalEmail + " Edited";

    // Homer's name and email should be populated.
    onView(withId(R.id.email)).check(matches(withText(originalEmail)));
    onView(withId(R.id.edit_name)) //
        .check(matches(withText(originalName)))
        // Edit his name.
        .perform(ViewActions.typeText(" Edited")) //
        .check(matches(withText(editedName)))
        // Continue to the next screen.
        .perform(ViewActions.pressImeActionButton());

    // Now we're on the EditEmailScreen. It's also an EditContactKey, so it should share the same
    // ancestor scopes as EditNameScreen. We should still see the same editor and storage.
    onView(withParent(withId(android.R.id.content))) //
        .check(hasFlowService(CONTACT_EDITOR)) //
        .check(hasFlowService(CONTACTS_STORAGE));

    // We can verify that we have the same editor instance by checking that we see the name change
    // we just made on the previous screen.
    onView(withId(R.id.name)).check(matches(withText(editedName)));
    // Email should also be populated.
    onView(withId(R.id.edit_email)) //
        .check(matches(withText(originalEmail)))
        // Edit the email field.
        .perform(ViewActions.typeText(" Edited")) //
        .check(matches(withText(editedEmail)));
    // Save changes. This should take us back to the list.
    onView(withId(R.id.save)).perform(click());

    // The ContactsEditor should no longer be available, but we should still have storage.
    // TODO see if it's in the view's context
    onView(withParent(withId(android.R.id.content))) //
        .check(doesNotHaveFlowService(CONTACT_EDITOR)) //
        .check(hasFlowService(CONTACTS_STORAGE));

    // We can verify that we always had the same storage instance by checking that our
    // edits are reflected in the list.
    onData(contactWithId(HOMER.id)) //
        .onChildView(withId(R.id.contact_name)) //
        .check(matches(withText(editedName)));
    onData(contactWithId(HOMER.id)) //
        .onChildView(withId(R.id.contact_email)) //
        .check(matches(withText(editedEmail)));

    // Press back to go to the welcome screen.
    pressBack();

    // By going back to the WelcomeScreen, we leave the ContactsUiKey. This should cause the only
    // reference to the ContactsStorage to be dropped.
    onView(withParent(withId(android.R.id.content))) //
        .check(doesNotHaveFlowService(CONTACTS_STORAGE)) //
        .check(doesNotHaveFlowService(CONTACT_EDITOR));

    // Click through to go to the list again.
    onView(withText(new WelcomeScreen().toString())).perform(click());

    // Since we're re-entering the ContactsKey, we should get a brand
    // new ContactsStorage instance, and our edits should be lost.
    onView(withParent(withId(android.R.id.content))) //
        .check(hasFlowService(CONTACTS_STORAGE));
    onData(contactWithId(HOMER.id)) //
        .onChildView(withId(R.id.contact_name)) //
        .check(matches(withText(originalName)));
    onData(contactWithId(HOMER.id)) //
        .onChildView(withId(R.id.contact_email)) //
        .check(matches(withText(originalEmail)));
  }

  private static Matcher<Contact> contactWithId(String contactId) {
    final String contactId1 = contactId;
    return new BaseMatcher<Contact>() {
      private final String contactId = contactId1;

      @Override public boolean matches(Object o) {
        return o instanceof Contact && contactId.equals(((Contact) o).id);
      }

      @Override public void describeTo(Description description) {
        description.appendText("contact with id " + contactId);
      }
    };
  }
}

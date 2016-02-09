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

package flow.sample.multikey;

import android.content.res.Configuration;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;

public final class MultiKeySampleTest {

  @Rule public ActivityTestRule rule = new ActivityTestRule<>(MultiKeySampleActivity.class);

  @Test public void walkthrough() {
    // Start on ScreenOne.
    onView(withText(new ScreenOne().toString())).check(matches(isDisplayed()));

    // Click to show DialogScreen.
    onView(withText(new ScreenOne().toString())).perform(click());
    onView(withText(new DialogScreen(new ScreenTwo()).toString())) //
        .inRoot(isDialog()) //
        .check(matches(isDisplayed()));

    // We should still see ScreenOne behind the dialog.
    onView(withText(new ScreenOne().toString())) //
        .inRoot(withDecorView(is(rule.getActivity().getWindow().getDecorView())));

    // Let's rotate to make sure we keep the dialog and the view.
    rotate();

    // Should still see DialogScreen.
    onView(withText(new DialogScreen(new ScreenTwo()).toString())) //
        .inRoot(isDialog()) //
        .check(matches(isDisplayed()));
    onView(withText(new ScreenOne().toString())) //
        .inRoot(withDecorView(is(rule.getActivity().getWindow().getDecorView())));

    // Click Yes on dialog to advance to ScreenTwo.
    onView(withText("Yes")).inRoot(isDialog()).perform(click());

    // Should be on ScreenTwo.
    onView(withText(new ScreenTwo().toString())).check(matches(isDisplayed()));

    // Press back to go back to the dialog.
    pressBack();

    // Should see the Dialog over ScreenOne again.
    onView(withText(new DialogScreen(new ScreenTwo()).toString())) //
        .inRoot(isDialog()) //
        .check(matches(isDisplayed()));
    onView(withText(new ScreenOne().toString())) //
        .inRoot(withDecorView(is(rule.getActivity().getWindow().getDecorView())));

    // Click forward to ScreenTwo
    // Click Yes on dialog to advance to ScreenTwo.
    onView(withText("Yes")).inRoot(isDialog()).perform(click());

    // Click screen to go back to ScreenOne, skipping the dialog.
    onView(withText(new ScreenTwo().toString())).perform(click());
    onView(withText(new ScreenOne().toString())).check(matches(isDisplayed()));
  }

  private void rotate() {
    Configuration config =
        InstrumentationRegistry.getTargetContext().getResources().getConfiguration();
    rule.getActivity()
        .setRequestedOrientation(
            (config.orientation == ORIENTATION_PORTRAIT) ? SCREEN_ORIENTATION_LANDSCAPE
                : SCREEN_ORIENTATION_PORTRAIT);
  }
}

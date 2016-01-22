package flow.sample.basic;

import android.content.res.Configuration;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
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
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Verifies that simple navigation works, along with Flow and view state persistence across
 * navigation and across configuration changes.
 */
public class BasicSampleTest {

  @Rule public ActivityTestRule rule = new ActivityTestRule<>(BasicSampleActivity.class);

  /** Verifies that the app is in its default state on a cold start. */
  @Test public void defaultKeyIsUsed() {
    onView(withId(R.id.basic_activity_frame))
        .check(matches(hasDescendant(isAssignableFrom(WelcomeView.class))));
  }

  /**
   * Verifies that the current Flow state is maintained, as well as view state associated with
   * Flow state.
   */
  @Test public void rotationMaintainsState() {

    // Enter some text on the welcome screen
    onView(withId(R.id.welcome_screen_name))
        .perform(ViewActions.typeText("Bart"));

    rotate();

    // We should still have that text, despite the configuration change.
    onView(withId(R.id.welcome_screen_name))
        .check(matches(withText("Bart")));

    // Continue to the next screen and verify that it's showing info from our Flow state object.
    onView(withId(R.id.welcome_screen_name))
        .perform(ViewActions.typeText("\n"));
    onView(withId(R.id.hello_name))
        .check(matches(withText("Hello Bart")));

    // Change the text in the counter TextView. Only this view knows its state, we don't store it
    // anywhere else.
    onView(withId(R.id.hello_increment))
        .perform(click())
        .perform(click());
    onView(withId(R.id.hello_counter))
        .check(matches(withText("2")));

    rotate();

    // Verify that we still have our Flow state object.
    onView(withId(R.id.hello_name))
        .check(matches(withText("Hello Bart")));
    // Verify that the counter TextView's view state was restored.
    onView(withId(R.id.hello_counter))
        .check(matches(withText("2")));
  }

  /** Verifies that states in the history keep their associated view state. */
  @Test public void goingBackWorksAndRestoresState() {

    // Enter some text in the name field and go forward.
    // The field's view state, including the text we entered, should be remembered in the history.
    onView(withId(R.id.welcome_screen_name))
        .perform(ViewActions.typeText("Bart\n"));
    onView(withId(R.id.basic_activity_frame))
        .check(matches(hasDescendant(isAssignableFrom(HelloView.class))));

    pressBack();

    onView(withId(R.id.basic_activity_frame))
        .check(matches(hasDescendant(isAssignableFrom(WelcomeView.class))));

    // When we navigated back, the view state of the name field should have been restored.
    onView(withId(R.id.welcome_screen_name))
        .check(matches(withText("Bart")));
  }

  private void rotate() {
    Configuration config =
        InstrumentationRegistry.getTargetContext().getResources().getConfiguration();
    rule.getActivity().setRequestedOrientation(
        (config.orientation == ORIENTATION_PORTRAIT) ?
            SCREEN_ORIENTATION_LANDSCAPE : SCREEN_ORIENTATION_PORTRAIT);
  }
}

package flow.sample.helloworld;

import android.support.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class HelloTest {
  @Rule public ActivityTestRule rule = new ActivityTestRule<>(HelloWorldActivity.class);

  @Test public void hello() {
    onView(withText("Hello, World!")).perform(click());
  }
}

package flow.sample.intents;

import android.content.Context;
import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import flow.Flow;
import flow.History;
import flow.sample.intents.IntentsSingleInstanceSampleActivity;
import flow.sample.intents.IntentsStandardSampleActivity;
import flow.sample.intents.StringParceler;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static java.util.Arrays.asList;

/**
 * Demonstrates the use of Intents to drive espresso tests to specific screens.
 */
public class IntentsSampleTest {

  @Rule public ActivityTestRule rule = new ActivityTestRule<>(IntentsSingleInstanceSampleActivity.class);

  @Test public void singleInstanceActivity() {
    List<String> expected = asList("Able", "Baker", "Charlie");
    History h = History.emptyBuilder().pushAll(expected).build();

    Context context = getInstrumentation().getTargetContext().getApplicationContext();
    Intent intent = new Intent(context, IntentsSingleInstanceSampleActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    Flow.addHistory(intent, h, new StringParceler());
    context.startActivity(intent);

    onView(withText("Charlie")).perform(pressBack());
    onView(withText("Baker")).perform(pressBack());
    onView(withText("Able")).perform(click());
  }

  @Test public void standardActivity() {
    List<String> expected = asList("Higgledy", "Piggledy", "Pop");
    History h = History.emptyBuilder().pushAll(expected).build();

    Context context = getInstrumentation().getTargetContext().getApplicationContext();
    Intent intent = new Intent(context, IntentsStandardSampleActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    Flow.addHistory(intent, h, new StringParceler());
    context.startActivity(intent);

    onView(withText("Pop")).perform(pressBack());
    onView(withText("Piggledy")).perform(pressBack());
    onView(withText("Higgledy")).perform(click());
  }
}

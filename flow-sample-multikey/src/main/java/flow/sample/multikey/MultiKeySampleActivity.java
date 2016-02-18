package flow.sample.multikey;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import flow.Direction;
import flow.Flow;
import flow.KeyChanger;
import flow.KeyDispatcher;
import flow.State;
import flow.TraversalCallback;
import java.util.Map;

/**
 * Demonstrates MultiKeys, e.g. screens with dialogs.
 */
public class MultiKeySampleActivity extends AppCompatActivity {
  @Override protected void attachBaseContext(Context baseContext) {
    baseContext = Flow.configure(baseContext, this)
        .dispatcher(KeyDispatcher.configure(this, new MyKeyChanger()).build())
        .defaultKey(new ScreenOne())
        .install();
    super.attachBaseContext(baseContext);
  }

  @Override public void onBackPressed() {
    if (!getFlow().goBack()) {
      super.onBackPressed();
    }
  }

  class MyKeyChanger extends KeyChanger {
    Dialog visibleDialog;

    @Override public void changeKey(@Nullable State outgoingState, State incomingState,
        Direction direction, Map<Object, Context> incomingContexts,
        TraversalCallback callback) {

      final Object mainKey;
      final Object dialogKey;

      final Object showThis = incomingState.getKey();
      if (showThis instanceof DialogScreen) {
        mainKey = ((DialogScreen) showThis).mainContent;
        dialogKey = showThis;
      } else {
        mainKey = showThis;
        dialogKey = null;
      }

      final TextView mainView = new TextView(incomingContexts.get(mainKey));
      if (mainKey instanceof ScreenOne) {
        mainView.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View view) {
            getFlow().set(new DialogScreen(mainKey));
          }
        });
      } else {
        mainView.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View view) {
            getFlow().set(new ScreenOne());
          }
        });
      }
      mainView.setText(mainKey.toString());

      setContentView(mainView);

      dismissOldDialog();
      if (dialogKey != null) {
        visibleDialog = new AlertDialog.Builder(incomingContexts.get(dialogKey)) //
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialogInterface, int i) {
                getFlow().goBack();
              }
            }) //
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
              @Override public void onCancel(DialogInterface dialogInterface) {
                getFlow().goBack();
              }
            }) //
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialogInterface, int i) {
                getFlow().set(new ScreenTwo());

                // In real life you'd be more likely to do something like this,
                // to prevent the dialog from showing up again when the back
                // button is hit.
                //
                //final History.Builder newHistory = getFlow().getHistory().buildUpon();
                //newHistory.pop(); // drop the dialog
                //newHistory.push(new ScreenTwo());
                //getFlow().setHistory(newHistory.build(), Flow.Direction.FORWARD);
              }
            }) //
            .setTitle(dialogKey.toString()) //
            .show();

        // Prevent logging of android.view.WindowLeaked.
        getApplication().registerActivityLifecycleCallbacks(new ActivityLifecycleCallbackAdapter() {
          @Override public void onActivityDestroyed(Activity activity) {
            getApplication().unregisterActivityLifecycleCallbacks(this);
            dismissOldDialog();
          }
        });
      }

      callback.onTraversalCompleted();
    }

    private void dismissOldDialog() {
      if (visibleDialog != null) {
        visibleDialog.dismiss();
        visibleDialog = null;
      }
    }
  }

  private Flow getFlow() {
    return Flow.get(MultiKeySampleActivity.this);
  }
}

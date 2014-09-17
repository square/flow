package com.example.flow.screenswitcher;

/**
 * Implemented by views that want the option to intercept back button taps. If a view has subviews
 * that implement this interface their {@link #onBackPressed()} method should be invoked before
 * any of this view's own logic.
 * <p/>
 *
 * The typical flow of back button handling starts in the {@link android.app.Activity#onBackPressed()}
 * calling {@link #onBackPressed()} on its content view. Each view in turn delegates to its
 * child views to give them first say.
 */
public interface HandlesBack {
  /**
   * Returns <code>true</code> if back event was handled, <code>false</code> if someone higher in
   * the chain should.
   */
  boolean onBackPressed();
}

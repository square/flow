Change Log
==========

Version 0.9 *(tbd)*
-------
  * API break: reset / replace methods more consistent, with explicit control over direction.
     * resetTo > resetBackTo
     * replaceTo > resetToUpStackOf
     * forward, backward > resetTo(Object, Direction) and resetTo(BackStack, Direction)

Version 0.8 *(2014-09-17)*
-------
  * API break: The Listener now gets a Callback, which it *must* call when it has completed a
    transition.
  * Flow now supports reentry.  While a Listener is executing, calls to Flow which modify the
    backstack are enqueued.
  * Beefed up sample app, including demonstration of providing view persistence via
    the back stack

Version 0.7 *(2014-05-16)*
-------
  * replaceTo and goUp keep original screens for a matching prefix of the stack.
  * Fix waitForMeasureLoop in example code.

Version 0.6 *(2014-04-21)*
-------
  * API break: replaceTo() now has a new Direction associated with it, `REPLACE`.
    This is logically more correct because the incumbent backstack is not
    consulted, and convenient because a replace transition is typically
    different from a forward or backward transition.

Version 0.5 *(2014-04-15)*
-------
  * Keep original screen on stack if found by resetTo.

Version 0.4 *(2014-01-28)*
-------
  * API break: @Screen(layout=R.layout.foo) > @Layout(R.layout.foo), and Screens > Layouts.
    Support for view class literals is gone. They break theming and the fix isn't worth the bother.

Version 0.3 *(2014-01-21)*
-------
  * New: Backstack#fromUpChain(Object), allows backstack to be created from
    a HasParent screen.

Version 0.2 *(2013-11-12)*
-------

Initial release.

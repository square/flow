Change Log
==========

Version 0.10 *(2015-05-01)*
------
* Fix: The Builder returned by `History#buildUpon()` is now safer to use. See
  javadoc for detail.

Version 0.9 *(2015-04-24)*
------
A large number of breaking changes have been made in the interest of focusing 
the library.

* Backstack is now called History and has some new method names.
* The `resetTo`, `goTo`, `replaceTo`, `forward`, and `backward` operations are 
  all gone. In their place are two simple methods: `set(Object)` and 
  `set(History, Direction)`.
* `HasParent` and `goUp` are gone. "Up" navigation is left as an exercise to app
  authors who need it, at least for the time being.
* The `@Layout` annotation has been removed. You can find it in the sample if
  you want a copy.
* Listener is now called Dispatcher, and can be set on a Flow after
  construction. Dispatcher gets more information than Listener did.

There are also some new features, and more are coming. 

* Added a Context service for easily obtaining the Flow.
* Added `FlowDelegate` for easier integration into an Activity.
* Added the flow-path module. Paths generally represent states/screens in an app, and
  are associated with Contexts which can be created by a user-supplied factory.
  PathContainer helps with switching views while maintaining view state.

Version 0.8 *(2014-09-17)*
-------
* API break: The Listener now gets a Callback, which it *must* call when it has
  completed a transition.
* Flow now supports reentry.  While a Listener is executing, calls to Flow which modify
  the backstack are enqueued.
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
  Support for view class literals is gone. They break theming and the fix isn't worth
  the bother.

Version 0.3 *(2014-01-21)*
-------
* New: Backstack#fromUpChain(Object), allows backstack to be created from a HasParent
  screen.

Version 0.2 *(2013-11-12)*
-------
Initial release.

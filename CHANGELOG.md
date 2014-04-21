Change Log
==========

Version 0.6 *TBD*
----------------------------
  * API break: replaceTo() now has a new Direction associated with it, `REPLACE`.
    This is logically more correct because the incumbent backstack is not
    consulted, and convenient because a replace transition is typically
    different from a forward or backward transition.

Version 0.5 *(2014-04-15)*
----------------------------
  * Keep original screen on stack if found by resetTo.

Version 0.4 *(2014-01-28)*
----------------------------
  * API break: @Screen(layout=R.layout.foo) > @Layout(R.layout.foo), and Screens > Layouts.
    Support for view class literals is gone. They break theming and the fix isn't worth the bother.

Version 0.3 *(2014-01-21)*
----------------------------
  * New: Backstack#fromUpChain(Object), allows backstack to be created from
    a HasParent screen.

Version 0.2 *(2013-11-12)*
----------------------------

Initial release.

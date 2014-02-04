Change Log
==========

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

# Deprecated

Flow had a good run and served us well, but new use is strongly discouraged. The app suite at Square that drove its creation is in the process of replacing Flow with [Square Workflow](https://square.github.io/workflow/).

# Flow

_"Name-giving will be the foundation of our science."_ &mdash; Linnaeus

_"The winds and waves are always on the side of the ablest navigators."_ &mdash; Gibbon

_"Memory is the treasury and guardian of all things._" &mdash; Cicero

**Flow gives names to your Activity's UI states, navigates between them, and remembers where it's been.**

## Features

Navigate between UI states. Support the back button easily without confusing your users with surprising results.

Remember the UI state, and its history, as you navigate and across configuration changes and process death.

Manage resources with set-up/tear-down hooks invoked for each UI state. UI states can easily share resources, and they'll be disposed when no longer needed.

Manage all types of UIs-- complex master-detail views, multiple layers, and window-based dialogs are all simple to manage.


## Using Flow

Gradle:

```groovy
compile 'com.squareup.flow:flow:1.0.0-alpha3'
```

Install Flow into your Activity:

```java
public class MainActivity {
  @Override protected void attachBaseContext(Context baseContext) {
    baseContext = Flow.configure(baseContext, this).install();
    super.attachBaseContext(baseContext);
  }
}
```

By default, Flow will take over your Activity's content view. When you start your Activity, you should see a "Hello world" screen. Of course you'll want to change this-- that's covered under [Controlling UI](#controlling-ui) below.

### Defining UI states with key objects

Your Activity's UI states are represented in Flow by Objects, which Flow refers to as "keys". Keys are typically [value objects][valueobject] with just enough information to identify a discrete UI state.

Flow relies on a key's [equals][equals] and [hashCode][hashcode] methods for its identity. Keys should be immutable-- that is, their `equals` and `hashCode` methods should always behave the same.

To give an idea of what keys might look like, here are some examples:

```java
public enum TabKey {
  TIMELINE,
  NOTIFICATIONS,
  PROFILE
}

public final class HomeKey extends flow.ClassKey {
}

public final class ArticleKey {
  public final String articleId;

  public ArticleKey(String articleId) {
    this.articleId = articleId;
  }

  public boolean equals(Object o) {
    return o instanceof ArticleKey
        && articleId.equals(((ArticleKey) o).articleId);
  }
  
  public int hashCode() {
    return articleId.hashCode();
  }
}
```

See the [Sample Projects](#sample-projects) below for more example keys.


### Navigation and History
Flow offers simple commands for navigating within your app.

`Flow#goBack()` -- Goes back to the previous [key][keys]. Think "back button".

`Flow#set(key)` -- Goes to the requested key. Goes back or forward depending on whether the key is already in the History.

Flow also lets you rewrite history safely and easily.

`Flow#setHistory(history, direction)` -- Change history to whatever you want.

See the [Flow][Flow.java] class for other convenient operators.

As you navigate the app, Flow keeps track of where you've been. And Flow makes it easy to save view state (and any other state you wish) so that when your users go back to a place they've been before, it's just as they left it.

### Controlling UI
Navigation only counts if it changes UI state. Because every app has different needs, Flow lets you plug in [your own logic][Dispatcher.java] for responding to navigation and updating your UI.

See the Basic Sample, Tree Sample, and MultiKey Sample [below](#sample-projects) for examples.

### Managing resources
Your app requires different resources when it's in different states; sometimes those resources are shared between states. Flow [makes it easy][ServicesFactory.java] to associate resources with keys so they're set up when needed and torn down (only) when they're not anymore.

See the Tree Sample for an [example][FlowServices.java].

### Surviving configuration changes and process death
Android is a hostile environment. One of its greatest challenges is that your Activity or even your process can be destroyed and recreated under a variety of circumstances. Flow makes it easy to weather the storm, by automatically remembering your app's state and its history. 

You [supply the serialization][KeyParceler.java] for your keys, and Flow does the rest. Flow  automatically saves and restores your History (including any state you've saved), taking care of all of the Android lifecycle events so you don't have to worry about them.

## Sample projects

* [Hello World](flow-sample-helloworld) - A starting point for integration.
* [Basic Sample](flow-sample-basic) - Fully configured Flow.
* [Tree Sample](flow-sample-tree) - Uses TreeKeys to define scopes and share state.
* [MultiKey Sample](flow-sample-multikey) - Uses MultiKeys to represent screens with dialogs as discrete states.

## License

    Copyright 2013 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[Dispatcher.java]: flow/src/main/java/flow/Dispatcher.java
[equals]: http://developer.android.com/reference/java/lang/Object.html#equals(java.lang.Object)
[Flow.java]: flow/src/main/java/flow/Flow.java
[FlowServices.java]: flow-sample-tree/src/main/java/flow/sample/tree/FlowServices.java
[hashcode]: http://developer.android.com/reference/java/lang/Object.html#hashCode()
[KeyParceler.java]: https://github.com/square/flow/blob/master/flow/src/main/java/flow/KeyParceler.java
[keys]: #defining-ui-states-with-key-objects
[ServicesFactory.java]: flow/src/main/java/flow/ServicesFactory.java
[valueobject]: https://en.wikipedia.org/wiki/Value_object

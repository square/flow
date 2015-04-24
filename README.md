# Flow

Flow allows you to enumerate to your app's UI states and navigate between them.

You represent each state of your app as a value object.

```java
public final class TrackScreen {
  public final String albumId;
  public final String trackId;

  public TrackScreen(String albumId, String trackId) {
    this.albumId = albumId;
    this.trackId = trackId;
  }
}
```

Ask Flow to put your app into a state by calling `Flow#set()`.

```java
flow.set(new TrackScreen(albumId, trackId));
```

Flow keeps track of your state history, so you can go back.

```java
flow.goBack();
```

Your app provides Flow with a Dispatcher which executes state changes.

```java
flow.setDispatcher(new Flow.Dispatcher() {
 @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
      Object newState = traversal.destination.top();
      displayViewFor(newState);
      callback.onTraversalCompleted();
    }
});
```


Download
--------

Download [the latest JAR][1] or grab via Maven:

```xml
<dependency>
    <groupId>com.squareup.flow</groupId>
    <artifactId>flow</artifactId>
    <version>(insert latest version)</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.squareup.flow:flow:(latest version)'
```



License
--------

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




 [1]: http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.squareup.flow&a=flow&v=LATEST

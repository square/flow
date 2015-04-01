# Flow

Flow allows you to enumerate to your app's UI states and navigate between them.

## Path

A Path object maps to a distinct UI state of your app. It contains just enough information to
recreate that state.

```java
@Layout(R.layout.track)
public class TrackScreen extends Path {
  public final int albumId;
  public final int trackId;

  public TrackScreen(int albumId, int trackId) {
    this.albumId = albumId;
    this.trackId = trackId;
  }
}
```

## Backstack

The Backstack is the history of Paths, with the head being the current Path.

## Flow

The Flow holds the current Backstack and offers navigation.

```java
flow.set(new TrackScreen(albumId, trackId));

flow.goBack();
```

## Dispatcher
Your app provides Flow with a Dispatcher which executes UI state changes.

```java
flow.setDispatcher(new Flow.Dispatcher() {
 @Override public void dispatch(Traversal traversal, TraversalCallback callback) {
      Path newPath = traversal.destination.current();
      displayViewFor(newPath);
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

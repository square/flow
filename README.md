# Flow

Flow is a small library that helps with describing an app as a collection of moderately independent screens. These screens can be pushed onto a concrete backstack to provide navigation history.

## Screen

A screen describes a distinct state of an application. It contains enough information to bootstrap the view.

```java
@Layout(R.layout.track)
public class TrackScreen implements HasParent<AlbumScreen> {
  public final int albumId;
  public final int trackId;

  @Override public AlbumScreen getParent() {
    return new AlbumScreen(albumId);
  }

  public TrackScreen(int albumId, int trackId) {
    this.albumId = albumId;
    this.trackId = trackId;
  }
}
```

The `HasParent` interface is used to support the *up* notion used in Android.

## Backstack

The backstack is the history of screens, with the head being the current or last-most screen.

## Flow

The flow holds the current truth about your application, the history of screens. It can be told to transition to another screen by simply instantiating the screen you want to go to.

```java
flow.goTo(new TrackScreen(albumId, trackId));
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

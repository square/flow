# Flow

Flow is a small library that helps with describing an app as a collection of moderately independent screens. These screens can be pushed onto a concrete backstack to provide navigation history.

## Screen

A screen describes a distinct state of an application. It contains enough information to bootstrap the view.

```java
  @Layout(R.layout.track)
  public class TrackScreen implements Screen, HasParent<AlbumScreen> {
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
  flow.goTo(new TrackScreen(albumId, trackId);
```

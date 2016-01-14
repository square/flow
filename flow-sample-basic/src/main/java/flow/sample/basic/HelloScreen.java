package flow.sample.basic;

import android.os.Parcel;
import android.os.Parcelable;

final class HelloScreen implements Parcelable {
  final String name;

  HelloScreen(String name) {
    this.name = name;
  }

  protected HelloScreen(Parcel in) {
    name = in.readString();
  }

  public static final Creator<HelloScreen> CREATOR = new Creator<HelloScreen>() {
    @Override public HelloScreen createFromParcel(Parcel in) {
      return new HelloScreen(in.readString());
    }

    @Override public HelloScreen[] newArray(int size) {
      return new HelloScreen[size];
    }
  };

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(name);
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HelloScreen that = (HelloScreen) o;

    return name.equals(that.name);
  }

  @Override public int hashCode() {
    return name.hashCode();
  }
}

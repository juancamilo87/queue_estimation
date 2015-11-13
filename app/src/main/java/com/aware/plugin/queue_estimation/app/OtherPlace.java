package com.aware.plugin.queue_estimation.app;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;
import java.util.Locale;

/**
 * Created by researcher on 22/07/15.
 */
public class OtherPlace implements Parcelable, Place{

    private String name;
    private String id;
    private double distance;

    public OtherPlace()
    {
        name = "Other";
        id = "otherPlaceId";
        distance = 100000;
    }

    public OtherPlace(String name, String id, double distance)
    {
        this.name = name;
        this.id = id;
        this.distance = distance;
    }

    private OtherPlace(Parcel in) {
        name = in.readString();
        id = in.readString();
        distance = in.readDouble();
    }

    public double getDistance()
    {
        return distance;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Integer> getPlaceTypes() {
        return null;
    }

    @Override
    public CharSequence getAddress() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public CharSequence getName() {
        return name;
    }

    @Override
    public LatLng getLatLng() {
        return null;
    }

    @Override
    public LatLngBounds getViewport() {
        return null;
    }

    @Override
    public Uri getWebsiteUri() {
        return null;
    }

    @Override
    public CharSequence getPhoneNumber() {
        return null;
    }

    @Override
    public float getRating() {
        return 0;
    }

    @Override
    public int getPriceLevel() {
        return 0;
    }

    @Override
    public Place freeze() {
        return this;
    }

    @Override
    public boolean isDataValid() {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(id);
        dest.writeDouble(distance);
    }

    public static final Parcelable.Creator<OtherPlace> CREATOR
            = new Parcelable.Creator<OtherPlace>() {

        // This simply calls our new constructor (typically private) and
        // passes along the unmarshalled `Parcel`, and then returns the new object!
        @Override
        public OtherPlace createFromParcel(Parcel in) {
            return new OtherPlace(in);
        }

        // We just need to copy this and change the type to match our class.
        @Override
        public OtherPlace[] newArray(int size) {
            return new OtherPlace[size];
        }
    };
}

package com.aware.plugin.tracescollector.app;

import com.google.android.gms.location.places.Place;

/**
 * Created by researcher on 02/06/15.
 */
public class MyPlace {

    private Place place;

    public MyPlace(Place nPlace)
    {
        place = nPlace.freeze();
    }

    @Override
    public String toString() {
        return place.getName().toString();
    }

    public Place getPlace()
    {
        return place;
    }
}

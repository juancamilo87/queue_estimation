package com.aware.plugin.queue_estimation.app;

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

    public double getDistance()
    {
        if(place instanceof OtherPlace)
        {
            return ((OtherPlace) place).getDistance();
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof MyPlace && place.getId().equals(((MyPlace) o).getPlace().getId()))
        {
            return true;
        }
        return false;
    }
}

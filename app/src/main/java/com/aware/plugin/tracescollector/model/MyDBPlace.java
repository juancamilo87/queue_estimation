package com.aware.plugin.tracescollector.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by researcher on 29/06/15.
 */
public class MyDBPlace {

    private String name;
    private LatLng latLng;
    private String id;
    private int waitTime;

    public MyDBPlace(String name, LatLng latLng, String id) {
        this.name = name;
        this.latLng = latLng;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public String getId() {
        return id;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Integer waitTime) {
        if(waitTime == null)
            this.waitTime = 0;
        else
            this.waitTime = waitTime;
    }

    @Override
    public String toString() {
        return id;
    }
}

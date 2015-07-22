package com.aware.plugin.tracescollector.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by researcher on 29/06/15.
 */
public class MyDBPlace {

    private String name;
    private LatLng latLng;
    private String id;
    private double waitTime;
    private long last_update;

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

    public double getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(Double waitTime) {
        if(waitTime == null)
            this.waitTime = 0;
        else
            this.waitTime = waitTime;
    }

    public long getLast_update() {
        return last_update;
    }

    public void setLast_update(long last_update) {
        this.last_update = last_update;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    @Override
    public String toString() {
        return id;
    }
}

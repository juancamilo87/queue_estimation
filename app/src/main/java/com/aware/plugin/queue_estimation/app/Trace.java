package com.aware.plugin.queue_estimation.app;

/**
 * Created by researcher on 28/07/15.
 */
public class Trace {

    final private String device_id;
    final private String venue_id;
    final private String event;
    final private String other;
    final private long timestamp;

    public Trace(String device_id, String venue_id, String event, String other, long timestamp) {
        this.device_id = device_id;
        this.venue_id = venue_id;
        this.event = event;
        this.other = other;
        this.timestamp = timestamp;
    }

    public String getDevice_id() {
        return device_id;
    }

    public String getVenue_id() {
        return venue_id;
    }

    public String getEvent() {
        return event;
    }

    public String getOther() {
        return other;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

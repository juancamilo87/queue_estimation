package com.aware.plugin.queuetracescollector.app;

import android.util.Log;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by researcher on 22/06/15.
 */
public class Message {

    private String alias;
    private String message;
    private long time;

    public Message(String alias, String message, long time) {
        this.alias = alias;
        this.message = message;
        this.time = time;
        Log.d("time stored",this.time+"");
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getNormalTime()
    {
        Timestamp timestamp = new Timestamp(time*1000);
        Calendar localTime = Calendar.getInstance(TimeZone.getDefault());
        localTime .setTimeInMillis(timestamp.getTime());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        return simpleDateFormat.format(localTime.getTime());
    }
}

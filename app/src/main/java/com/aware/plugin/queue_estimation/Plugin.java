package com.aware.plugin.queue_estimation;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

public class Plugin extends Aware_Plugin {

    public static ContextProducer context_producer;


    @Override
    public void onCreate() {
        super.onCreate();
        if( DEBUG ) Log.d(TAG, "Traces Collector plugin running");

//        Aware.setSetting(this, Aware_Preferences.WEBSERVICE_SERVER,"httpfafahr;3ofe");

        //Activate any sensors/plugins you need here
        //...

        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };
        context_producer = CONTEXT_PRODUCER;
        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.TracesCollector_Data.CONTENT_URI };

//        Intent aware = new Intent(this, Aware.class);
//        startService(aware);
//        Aware.startPlugin(this, "com.aware.plugin.queue_estimation");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
        TAG = "Queue_Estimation";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if( DEBUG ) Log.d(TAG, "Traces collector plugin terminated");
//        Aware.setSetting(this, Settings.STATUS_PLUGIN_TRACESCOLLECTOR, false);

        //Deactivate any sensors/plugins you activated here
        //...

        //Ask AWARE to apply your settings
        //sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
//        Aware.stopPlugin(this, getPackageName());

        Aware.stopPlugin(this, "com.aware.plugin.queue_estimation");
    }
}

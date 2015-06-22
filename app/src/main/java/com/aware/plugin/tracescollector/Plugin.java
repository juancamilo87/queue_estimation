package com.aware.plugin.tracescollector;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class Plugin extends Aware_Plugin {


    public static final int MESSAGE_READ = 1837;

    private final IBinder mBinder = new LocalBinder();

    public static ContextProducer context_producer;


    @Override
    public void onCreate() {
        super.onCreate();
        if( DEBUG ) Log.d(TAG, "Traces Collector plugin running");
        //Initialize our plugin's settings
        if( Aware.getSetting(this, Settings.STATUS_PLUGIN_TRACESCOLLECTOR).length() == 0 ) {
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TRACESCOLLECTOR, true);
        }



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

        Aware.startPlugin(this, getPackageName());

        //Ask AWARE to apply your settings
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
        TAG = "Template";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if( DEBUG ) Log.d(TAG, "Traces collector plugin terminated");
        Aware.setSetting(this, Settings.STATUS_PLUGIN_TRACESCOLLECTOR, false);

        //Deactivate any sensors/plugins you activated here
        //...

        //Ask AWARE to apply your settings
        //sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
        Aware.stopPlugin(this, getPackageName());
    }


    @Override
    public IBinder onBind(Intent intent) {
        startService(new Intent(this, Plugin.class));
        return mBinder;
    }
    //returns the instance of the service
    public class LocalBinder extends Binder {
        public Plugin getServiceInstance(){
            return Plugin.this;
        }
    }

    //Here Activity register to the service as Callbacks client
//    public void registerClient(Activity activity){
//        this.activity = (Callbacks)activity;
//    }

    //callbacks interface for communication with service clients!
    public interface Callbacks{
        public void updateClient(long data);
    }

    private static void storeData(String tag_1, String tag_2, String tag_3, Context context)
    {
        ContentValues data = new ContentValues();
        data.put(Provider.TracesCollector_Data.TIMESTAMP, System.currentTimeMillis());
        data.put(Provider.TracesCollector_Data.DEVICE_ID, Aware.getSetting(context.getApplicationContext(), Aware_Preferences.DEVICE_ID));
        data.put(Provider.TracesCollector_Data.TAG_1, tag_1);
        data.put(Provider.TracesCollector_Data.TAG_2, tag_2);
        data.put(Provider.TracesCollector_Data.TAG_3, tag_3);

        context.getContentResolver().insert(Provider.TracesCollector_Data.CONTENT_URI, data);

        context_producer.onContext();
    }
}

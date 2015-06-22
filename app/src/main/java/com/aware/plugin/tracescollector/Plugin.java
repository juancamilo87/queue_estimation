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

    public String uuid;

    private BluetoothAdapter mBluetoothAdapter;

    private ConnectedThread conn;

    private final IBinder mBinder = new LocalBinder();

    public static ContextProducer context_producer;

    private ServerAcceptThread serverAcceptThread;


    @Override
    public void onCreate() {
        super.onCreate();
        if( DEBUG ) Log.d(TAG, "Traces Collector plugin running");
        //Initialize our plugin's settings
        if( Aware.getSetting(this, Settings.STATUS_PLUGIN_TRACESCOLLECTOR).length() == 0 ) {
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TRACESCOLLECTOR, true);
        }

        if(Aware.getSetting(this, Settings.BT_UUID_KEY).equals(""))
        {
            uuid = "cfa37877-e7a1-41a8-9673-2b0844b5868f";
        }
        else
        {
            uuid = Aware.getSetting(this, Settings.BT_UUID_KEY);
            Log.d("UUID", "Got UUID from AWARE settings");
        }

        //Activate any sensors/plugins you need here
        //...
        serverAcceptThread = new ServerAcceptThread();

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



        if(!connected())
        {
            Intent notificationIntent = new Intent(HelperActivity.NOTIFICATION_ACTION);
            sendOrderedBroadcast(notificationIntent, null);
            try{
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                    Log.d("BT Connection","Bluetooth is not enabled");
                }
                else
                {
                    if(serverAcceptThread != null)
                    {
                        try{
                            serverAcceptThread.cancel();
                            serverAcceptThread.interrupt();
                        }catch(Exception e){
                            Log.d("TCError","Error canceling thread");
                        }
                    }
                    try{
                        serverAcceptThread = new ServerAcceptThread();
                        serverAcceptThread.start();
                    }catch (Exception e){
                        Log.d("TCError","Error starting thread");
                    }
                }

            }catch(Exception e){}

        }
        else
        {
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            int mNotificationId = 13548;

            mNotifyMgr.cancel(mNotificationId);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if( DEBUG ) Log.d(TAG, "Traces collector plugin terminated");
        Aware.setSetting(this, Settings.STATUS_PLUGIN_TRACESCOLLECTOR, false);

        //Deactivate any sensors/plugins you activated here
        //...
        try{
            serverAcceptThread.cancel();
        }catch(Exception e){}

        //Ask AWARE to apply your settings
        //sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
        Aware.stopPlugin(this, getPackageName());
    }



    private class ServerAcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public ServerAcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            Log.d("TCError","Thread created");
            try {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("test", UUID.fromString(uuid));
            } catch (IOException e) {
                Log.d("TCError","Error listening");}
            mmServerSocket = tmp;
        }

        public void run() {



            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    Log.d("TCError","starting to accept");
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.d("TCError","Error accepting IO");
                    break;
                } catch (Exception e)
                {
                    Log.d("TCError","Error accepting G");
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)

                    manageConnectedSocket(socket);

                    try{
                        mmServerSocket.close();

                    } catch (IOException e) {
                        Log.d("TCError","Error closing ssocket");
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d("TCError","Error closing socket IO");}
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket)
    {
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int mNotificationId = 13548;

        mNotifyMgr.cancel(mNotificationId);
        conn = new ConnectedThread(socket);

        conn.start();

        //Manage connection
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                mmOutStream.flush();
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        public boolean isConnected()
        {
            try {
                mmOutStream.write("alive".getBytes());
                mmOutStream.flush();
            } catch (IOException e) {
                return false;
            }
            return mmSocket.isConnected();
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<Plugin> mPlugin;

        public MyHandler(Plugin plugin) {
            mPlugin = new WeakReference<Plugin>(plugin);
        }

        @Override
        public void handleMessage(Message msg) {
            Plugin plugin = mPlugin.get();
            if (plugin != null) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        byte[] data = (byte[]) msg.obj;
                        String readMessage = new String((byte[]) msg.obj, 0, (int) msg.arg1).trim();
                        Log.d("Connection", readMessage);

                        if (readMessage != null) {
                            int count = readMessage.length() - readMessage.replace("#", "").length();
                            if (count == 4 && readMessage.startsWith("#") && readMessage.endsWith("#")) {
                                String[] information = readMessage.split("#", -1);
                                for (int i = 1; i < 4; i++) {
                                    Log.d("To Store", "TAG" + i + ": " + information[i]);
                                }

                                storeData(information[1], information[2], information[3], plugin);
                            }
                        }
                }
            }
        }
    }

    public MyHandler mHandler = new MyHandler(this);

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

    public void startConnection()
    {
        try{
            if(serverAcceptThread != null)
            {
                serverAcceptThread.cancel();
                serverAcceptThread.interrupt();
            }

        }catch(Exception e){
            Log.d("TCError","Error canceling thread force");
        }
        try{
            serverAcceptThread = new ServerAcceptThread();
            serverAcceptThread.start();
        }catch (Exception e){
            Log.d("TCError","Error starting thread force");
        }
    }

    public boolean connected()
    {
        if(conn != null)
        {
            return conn.isConnected();
        }

        return false;
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

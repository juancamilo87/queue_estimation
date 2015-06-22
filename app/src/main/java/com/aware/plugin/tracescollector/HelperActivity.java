package com.aware.plugin.tracescollector;

import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Created by researcher on 03/06/15.
 */
public class HelperActivity extends Activity implements Plugin.Callbacks{

    public static final int TIME_DISCOVERABLE = 300;
    public static final int DISCOVERABLE_REQUEST = 4;  // The request code
    public static final String NOTIFICATION_ID = "notification_id";
    public static final String NOTIFICATION_ACTION = "com.aware.plugin.tracescollector.notification";

    private Plugin plugin;

    private Intent serviceIntent;

    private ImageView led;
    private Button btn_connect;
    private Button btn_close;

    private Handler handler;

    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.helper_activity);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(getIntent().getIntExtra(NOTIFICATION_ID, -1));




        led = (ImageView) findViewById(R.id.helper_led);
        btn_connect = (Button) findViewById(R.id.helper_connect);
        btn_close = (Button) findViewById(R.id.helper_close);


        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, TIME_DISCOVERABLE);

                startActivityForResult(discoverableIntent, DISCOVERABLE_REQUEST);
            }
        });

        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        verifyConnected();

                    }
                });
                handler.postDelayed(this,1000);


            }
        };
        handler.post(runnable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to

        if(requestCode == DISCOVERABLE_REQUEST)
        {
            if(resultCode == TIME_DISCOVERABLE)
            {
                Toast.makeText(this, "Made discoverable",Toast.LENGTH_SHORT).show();
                plugin.startConnection();
//                finish();

            }
            if(resultCode == RESULT_CANCELED)
            {
                Toast.makeText(this, "Please set the device discoverable",Toast.LENGTH_SHORT).show();
            }
        }

    }



    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            //Toast.makeText(MainActivity.this, "onServiceConnected called", Toast.LENGTH_SHORT).show();
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            Plugin.LocalBinder binder = (Plugin.LocalBinder) service;
            plugin = binder.getServiceInstance(); //Get instance of your service!
            //plugin.registerClient(HelperActivity.this); //Activity register in the service as client for callabcks!
            Toast.makeText(HelperActivity.this, "Connected to service", Toast.LENGTH_SHORT).show();
            verifyConnected();
//            tvServiceState.setText("Connected to service...");
//            tbStartTask.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Toast.makeText(HelperActivity.this, "service disconnected", Toast.LENGTH_SHORT).show();
//            tvServiceState.setText("Service disconnected");
//            tbStartTask.setEnabled(false);
        }
    };

    @Override
    public void updateClient(long data) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        serviceIntent = new Intent(HelperActivity.this, Plugin.class);
        startService(serviceIntent);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!

        handler.removeCallbacks(runnable);
        handler.post(runnable);

        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_ACTION);
        filter.setPriority(2);
        registerReceiver(receiver,filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(runnable);
        try{
            unregisterReceiver(receiver);
        } catch (Exception e){}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnection != null) {
            unbindService(mConnection);
            mConnection = null;
        }
        handler.removeCallbacks(runnable);
    }

    private void verifyConnected()
    {
        if(plugin != null)
        {
            btn_connect.setEnabled(true);
            if(plugin.connected())
            {
                led.setImageResource(R.drawable.on_led);
                btn_connect.setVisibility(View.GONE);
                btn_close.setVisibility(View.VISIBLE);
            }
            else
            {
                led.setImageResource(R.drawable.off_led);
                btn_connect.setVisibility(View.VISIBLE);
                btn_connect.setEnabled(true);
                btn_close.setVisibility(View.GONE);
            }
        }
        else
        {
            led.setImageResource(R.drawable.off_led);
            btn_connect.setVisibility(View.VISIBLE);
            btn_connect.setEnabled(false);
            btn_close.setVisibility(View.GONE);
        }

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            abortBroadcast();
        }
    };
}

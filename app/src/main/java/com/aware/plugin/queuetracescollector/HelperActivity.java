package com.aware.plugin.queuetracescollector;

import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Created by researcher on 03/06/15.
 */
public class HelperActivity extends Activity{

    public static final int TIME_DISCOVERABLE = 300;
    public static final int DISCOVERABLE_REQUEST = 4;  // The request code
    public static final String NOTIFICATION_ID = "notification_id";
    public static final String NOTIFICATION_ACTION = "com.aware.plugin.queuetracescollector.notification";

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
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}

package com.aware.plugin.queue_estimation.app;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.ambient_noise.Plugin;
import com.aware.plugin.ambient_noise.Settings;
import com.aware.plugin.queue_estimation.Provider;
import com.aware.plugin.queue_estimation.R;
import com.aware.plugin.queue_estimation.db.LocationDataSource;
import com.aware.plugin.queue_estimation.db.PlacesDataSource;
import com.aware.plugin.queue_estimation.db.TempTracesDataSource;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.MapView;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class HomeScreen extends Activity {

    public static final String PREFS_NAME = "Preferences";
    public static final String PREF_QUEUING = "Queuing";
    public static final String PREF_IN_PLACE = "In_Place";
    public static final String PREF_VENUE_NAME = "Venue_name";
    public static final String PREF_VENUE_ID = "Venue_Id";
    public static final String PREF_MESSAGE_POSTED = "Message_posted";

    public static final String EVENT_ENTER = "ENTER";
    public static final String EVENT_START = "START";
    public static final String EVENT_STOP = "STOP";
    public static final String EVENT_EXIT = "EXIT";

    public static final String LOG_TAG = "COMAG.QUEUE";

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public static final int min_radius = 50;
    public static final long min_time = 3000;

    public static final int NOTIFICATION_ID = 53256;

    static final int PICK_VENUE_REQUEST = 1;  // The request code
    static final int MESSAGES_WALL = 2;  // The request code
    static final int PLACE_PICKER_REQUEST = 3;

    private static final String POST_TRACE_URL = "http://pan0166.panoulu.net/queue_estimation/post_trace.php";
    private static final String TRACES_CSV_FILE = "traces.csv";
    private static final String APP_FOLDER = "Queue_Estimation";

    private Context context;

    private ImageButton btn_start_stop;
    private TextView txt_start_stop;
    private ImageButton btn_enter_exit;
    private TextView txt_enter_exit;

    private RelativeLayout rl_start_stop;
    private RelativeLayout rl_enter_exit;

    private ProgressBar progress_circle;


    private boolean queuing;
    private boolean inPlace;

    private String venueName;
    private String venueId;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Location bestLocation;

    private long beginningTime;

    private Place venue;

    private SharedPreferences prefs;



    private boolean locating;


    private Button messages_btn;
    private Button map_btn;

    private boolean message_posted;

    private OkHttpClient client;

    private ImageButton context_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new OkHttpClient();
        context = this;
        setContentView(R.layout.activity_home_screen);

//        Intent aware = new Intent(getApplicationContext(), Aware.class);
//        startService(aware);

        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, true);
//        Aware.startPlugin(this, "com.aware.plugin.queue_estimation");

        context_button = (ImageButton) findViewById(R.id.quit_menu);
        registerForContextMenu(context_button);
        context_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openContextMenu(v);
            }
        });

        locating = false;
        prefs = getSharedPreferences(PREFS_NAME, 0);
        message_posted = prefs.getBoolean(PREF_MESSAGE_POSTED, false);
        queuing = prefs.getBoolean(PREF_QUEUING, false);
        inPlace = prefs.getBoolean(PREF_IN_PLACE, false);
        venueName = prefs.getString(PREF_VENUE_NAME, "");
        venueId = prefs.getString(PREF_VENUE_ID, "-1");

        rl_start_stop = (RelativeLayout) findViewById(R.id.home_start_stop);
        btn_start_stop = (ImageButton) findViewById(R.id.home_trigger_start_stop_button);
        txt_start_stop = (TextView) findViewById(R.id.home_text_start_stop);
        rl_enter_exit = (RelativeLayout) findViewById(R.id.home_enter_exit);
        btn_enter_exit = (ImageButton) findViewById(R.id.home_trigger_enter_exit_button);
        txt_enter_exit = (TextView) findViewById(R.id.home_text_enter_exit);
        progress_circle = (ProgressBar) findViewById(R.id.home_progress_circle);

        messages_btn = (Button) findViewById(R.id.home_messages_wall_btn);
        map_btn = (Button) findViewById(R.id.home_map_btn);

        initializeButtons();

        btn_enter_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterExitToggle();
            }
        });

        btn_start_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopToggle();
            }
        });

        messages_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MessagesActivity.class);
                intent.putExtra("venue_id", venueId);
                intent.putExtra("message_posted", message_posted);
                startActivityForResult(intent, MESSAGES_WALL);
            }
        });

        map_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, HeatMap.class);
                startActivity(intent);
            }
        });

//        // Fixing Later Map loading Delay
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    MapView mv = new MapView(getApplicationContext());
//                    mv.onCreate(null);
//                    mv.onPause();
//                    mv.onDestroy();
//                }catch (Exception ignored){
//
//                }
//
//                LocationDataSource lds = new LocationDataSource(getApplicationContext());
//                lds.open();
//                lds.cleanDB();
//                PlacesDataSource pds = new PlacesDataSource(getApplicationContext());
//                pds.open();
//                pds.cleanDB();
//            }
//        }).start();

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Context Menu");
        menu.add(0, v.getId(), 0, "Action 1");
        menu.add(0, v.getId(), 0, "Action 2");
        menu.add(0, v.getId(), 0, "Action 3");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle() == "Action 1") {
            Toast.makeText(this, "Action 1 invoked", Toast.LENGTH_SHORT).show();
            Aware.joinStudy(this, "https://api.awareframework.com/index.php/webservice/index/359/c7VKnU0IQPqD");

            Aware.startPlugin(this, "com.aware.plugin.queue_estimation");
        }
        else if (item.getTitle() == "Action 2") {
            Toast.makeText(this, "Action 2 invoked", Toast.LENGTH_SHORT).show();
        }
        else if (item.getTitle() == "Action 3") {
            Toast.makeText(this, "Action 3 invoked", Toast.LENGTH_SHORT).show();
        }
        else {
            return false;
        }
        return true;
    }

    private void enterExitToggle()
    {
        SharedPreferences.Editor editor = prefs.edit();

        if(inPlace)
        {
            inPlace = !inPlace;
            initializeButtons();
            sendData(EVENT_EXIT);
            venue = null;
            venueName = "";
            venueId = "-1";
            editor.putString(PREF_VENUE_NAME, venueName);
            editor.putString(PREF_VENUE_ID, venueId);
            showNotification(EVENT_EXIT);
            stopSensors();
        }
        else
        {
            inPlace = !inPlace;
            locating = true;
            getLocationForVenue();
            btn_enter_exit.setEnabled(false);
            txt_enter_exit.setText("");
            progress_circle.setVisibility(View.VISIBLE);
        }

        editor.putBoolean(PREF_IN_PLACE, inPlace);
        editor.commit();
    }

    private void stopSensors()
    {
        Log.d("QUEUE", "Stop sensors");

        Aware.setSetting(this, Aware_Preferences.STATUS_WIFI, false);
//        Aware.stopSensor(this, Aware_Preferences.STATUS_WIFI);
        Aware.setSetting(this, Aware_Preferences.STATUS_TELEPHONY, false);
//        Aware.stopSensor(this, Aware_Preferences.STATUS_TELEPHONY);
        Aware.setSetting(this, Settings.STATUS_PLUGIN_AMBIENT_NOISE, false);
        Aware.stopPlugin(this, "com.aware.plugin.ambient_noise");
        Intent aware = new Intent(getApplicationContext(), Aware.class);
        startService(aware);
//        Aware.stopPlugin(this, "com.aware.plugin.ambient_noise");
    }

    private void startSensors()
    {
        Log.d("QUEUE","Start sensors");
//        Intent awareIntent = new Intent(getApplicationContext(),Aware.class);
//        startService(awareIntent);
        Aware.setSetting(this, Aware_Preferences.STATUS_WIFI, true);
        Aware.setSetting(this, Aware_Preferences.FREQUENCY_WIFI, 300);
        Aware.startSensor(this, Aware_Preferences.STATUS_WIFI);
        Aware.setSetting(this, Aware_Preferences.STATUS_TELEPHONY, true);
        Aware.startSensor(this, Aware_Preferences.STATUS_TELEPHONY);
        Aware.setSetting(this, Settings.STATUS_PLUGIN_AMBIENT_NOISE, true);
        Aware.setSetting(this, Settings.FREQUENCY_PLUGIN_AMBIENT_NOISE, 5);
        Aware.setSetting(this, Settings.PLUGIN_AMBIENT_NOISE_SAMPLE_SIZE, 2);
        Aware.setSetting(this, Settings.PLUGIN_AMBIENT_NOISE_SILENCE_THRESHOLD, 50);
        Aware.startPlugin(this, "com.aware.plugin.ambient_noise");
    }

    private void startStopToggle()
    {
        SharedPreferences.Editor editor = prefs.edit();

        if(queuing)
        {
            queuing = !queuing;
            sendData(EVENT_STOP);
            initializeButtons();
            showNotification(EVENT_STOP);
        }
        else
        {
            queuing = !queuing;
            sendData(EVENT_START);
            initializeButtons();
            showNotification(EVENT_START);
            verifyMessages();
        }

        editor.putBoolean(PREF_QUEUING, queuing);
        editor.commit();
    }

    private void verifyMessages() {
        getMessagesRequest(venueId);
//        new GetMessagesTask().execute(venueId);
    }

    private void showMessagesInQueueDialog() {
        new AlertDialog.Builder(this)
                .setTitle("New Messages")
                .setMessage("Someone left messages for you.\n\nDo you want to see them?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent(context, MessagesActivity.class);
                        intent.putExtra("venue_id",venueId);
                        intent.putExtra("message_posted", message_posted);
                        startActivityForResult(intent, MESSAGES_WALL);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_chat_black_48dp)
                .show();
    }

    private void getLocationForVenue()
    {
//        Intent intent = new Intent(this, ChoosePlaceActivity.class);
//        startActivityForResult(intent, PICK_VENUE_REQUEST);

        beginningTime = System.currentTimeMillis();
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                //If no location store it
                if(bestLocation==null)
                {
                    bestLocation = location;
                }

                long timeDifference = location.getTime() - bestLocation.getTime();

                //If location is newer and more accurate than stored
                if(timeDifference > 0 && location.getAccuracy()<=bestLocation.getAccuracy())
                {
                    bestLocation = location;
                }
                //Determine accuracy
                long currentTime = System.currentTimeMillis();
                long timeSinceStart = currentTime - beginningTime;
                Log.d(HomeScreen.LOG_TAG, "Accuracy: " + location.getAccuracy());

                if(bestLocation.getAccuracy() <= min_radius && timeSinceStart != currentTime && timeSinceStart > min_time)
                {
                    showChooseVenueActivity(location);
                }

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        //Enable GPS locations
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        //Enable Network locations
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);


    }

    private void showChooseVenueActivity(Location location)
    {
        Log.d(HomeScreen.LOG_TAG, "latlng = (" + location.getLatitude() + "," + location.getLongitude() + ")");
//        Toast.makeText(this,"Latitude: "+location.getLatitude()+" - Longitude: "+location.getLongitude()+ " - Accuracy: " + location.getAccuracy(),Toast.LENGTH_SHORT).show();
        locationManager.removeUpdates(locationListener);
        bestLocation = null;
        beginningTime = 0;


        Intent intent = new Intent(this, ChoosePlaceActivity.class);
        startActivityForResult(intent, PICK_VENUE_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PICK_VENUE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
                progress_circle.setVisibility(View.GONE);
                locating = false;
                btn_enter_exit.setEnabled(true);
                venue = (Place) data.getParcelableExtra("venue");
                SharedPreferences.Editor editor = prefs.edit();
                venueName = venue.getName().toString();
                venueId = venue.getId();
                message_posted = false;
                editor.putString(PREF_VENUE_NAME, venueName);
                editor.putString(PREF_VENUE_ID, venueId);
                editor.putBoolean(PREF_MESSAGE_POSTED, message_posted);
                editor.commit();
                initializeButtons();

                Toast.makeText(this, "The venue is: " + venueName,Toast.LENGTH_SHORT).show();
                sendData(EVENT_ENTER);
                showNotification(EVENT_ENTER);
                startSensors();

            }
            if (resultCode == RESULT_CANCELED) {
                progress_circle.setVisibility(View.GONE);
                locating = false;
                inPlace = !inPlace;
                btn_enter_exit.setEnabled(true);
                initializeButtons();
                venue = null;
                SharedPreferences.Editor editor = prefs.edit();
                venueName = "";
                venueId = "1";
                editor.putString(PREF_VENUE_NAME,venueName);
                editor.putString(PREF_VENUE_ID, venueId);
                editor.putBoolean(PREF_IN_PLACE, inPlace);
                editor.commit();
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }

        }
        if(requestCode == MESSAGES_WALL)
        {
            if(resultCode == RESULT_OK)
            {
                SharedPreferences.Editor editor = prefs.edit();
                message_posted = true;
                editor.putBoolean(PREF_MESSAGE_POSTED, message_posted);
                editor.commit();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showNotification(String event)
    {
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        removeNotification();
        String message = "";
        String text = "You are currently ";
        switch (event)
        {
            case EVENT_ENTER:
                message = "Press to start queueing or exit the venue";
                break;
            case EVENT_START:
                message = "Press to stop queueing";
                text += "queueing ";
                break;
            case EVENT_STOP:
                message = "Press to start queueing or exit the venue";
                break;
            case EVENT_EXIT:
                removeNotification();
                return;
        }

        Intent resultIntent = new Intent(this, HomeScreen.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        if(venue != null)
            text += " in "+venue.getName()+".";

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon_remote_white)
                        .setContentTitle("Queue Estimator")
                        .setContentText(text)
                        .setContentIntent(pendingIntent)
                        .addAction(android.R.color.transparent, message, pendingIntent)
                        .setOngoing(true);
        // Sets an ID for the notification

        // Gets an instance of the NotificationManager service

        // Builds the notification and issues it.
        mNotifyMgr.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void removeNotification()
    {
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int mNotificationId = NOTIFICATION_ID;

        mNotifyMgr.cancel(mNotificationId);
    }

    private void initializeButtons()
    {
        if(!locating)
        {
            if(inPlace)
            {
                if(queuing)
                {
                    rl_enter_exit.setVisibility(View.GONE);
                    rl_start_stop.setVisibility(View.VISIBLE);
                    btn_start_stop.setImageResource(R.drawable.trigger_button_stop);
                    txt_start_stop.setText(getString(R.string.stop_button));
                    map_btn.setVisibility(View.GONE);
                    messages_btn.setVisibility(View.VISIBLE);
                }
                else
                {
                    rl_enter_exit.setVisibility(View.VISIBLE);
                    btn_enter_exit.setImageResource(R.drawable.trigger_button_stop);
                    txt_enter_exit.setText(getString(R.string.exit_button));
                    messages_btn.setVisibility(View.GONE);
                    map_btn.setVisibility(View.VISIBLE);
                    rl_start_stop.setVisibility(View.VISIBLE);
                    btn_start_stop.setImageResource(R.drawable.trigger_button_start);
                    txt_start_stop.setText(getString(R.string.start_button));
                }
            }
            else
            {
                rl_enter_exit.setVisibility(View.VISIBLE);
                btn_enter_exit.setImageResource(R.drawable.trigger_button_start);
                txt_enter_exit.setText(getString(R.string.enter_button));
                rl_start_stop.setVisibility(View.GONE);
                messages_btn.setVisibility(View.GONE);
                map_btn.setVisibility(View.VISIBLE);
            }
        }
    }

    private void sendData(String event)
    {
        ContentValues data = new ContentValues();
        long timestamp = System.currentTimeMillis();
        data.put(Provider.TracesCollector_Data.TIMESTAMP, timestamp);
        data.put(Provider.TracesCollector_Data.DEVICE_ID, Aware.getSetting(context.getApplicationContext(), Aware_Preferences.DEVICE_ID));
        data.put(Provider.TracesCollector_Data.TAG_1, venueId);
        data.put(Provider.TracesCollector_Data.TAG_2, event);
        data.put(Provider.TracesCollector_Data.TAG_3, "");

        context.getContentResolver().insert(Provider.TracesCollector_Data.CONTENT_URI, data);
        new UploadTraceTask(Aware.getSetting(context.getApplicationContext(), Aware_Preferences.DEVICE_ID),
                venueId, event, "", timestamp, this).execute();
    }

    private void enableUI()
    {
        btn_enter_exit.setEnabled(true);
        txt_enter_exit.setTextColor(getResources().getColor(android.R.color.black));
        btn_start_stop.setEnabled(true);
        txt_start_stop.setTextColor(getResources().getColor(android.R.color.black));
    }
    private void disableUI()
    {
        btn_enter_exit.setEnabled(false);
        txt_enter_exit.setTextColor(getResources().getColor(android.R.color.darker_gray));
        btn_start_stop.setEnabled(false);
        txt_start_stop.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeButtons();
    }

    private void getMessagesRequest(String... params)
    {


        String URL = MessagesActivity.getMessagesURL + "?venue_id="+params[0];

        try
        {
            // Create Request to server and get response
            Request request = new Request.Builder().url(URL).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e(HomeScreen.LOG_TAG, "Failed to get messages");
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    String jsonMessages = null;
                    if (response.code() == 200) {
                        String response_body = response.body().string();
                        Log.v(HomeScreen.LOG_TAG, "Your data: " + response_body);
                        jsonMessages = response_body;
                    } else {
                        Log.e(HomeScreen.LOG_TAG, "Failed to get messages");
                    }
                    try {
                        jsonMessages = "{array:" + jsonMessages + "}";
                        Log.d(HomeScreen.LOG_TAG, jsonMessages);
                        JSONObject jObject = new JSONObject(jsonMessages);
                        JSONArray jArray = jObject.getJSONArray("array");

                        if (jArray.length() > 0) {
                            new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    showMessagesInQueueDialog();
                                }
                            });

                        }
                    }catch(Exception e)
                    {

                    }
                }
            });

        }
        catch(Exception ex)
        {
            Log.e(HomeScreen.LOG_TAG, "Failed"); //response data
        }



    }

    private static class UploadTraceTask extends AsyncTask<Void, Void, Void>
    {
        private String deviceId;
        private String venueId;
        private String event;
        private String other;
        private long timestamp;
        private HomeScreen context;

        public UploadTraceTask(String deviceId, String venueId, String event, String other, long timestamp, HomeScreen context) {
            this.deviceId = deviceId;
            this.venueId = venueId;
            this.event = event;
            this.other = other;
            this.timestamp = timestamp;
            this.context = context;
        }

        @Override
        protected Void doInBackground(Void... params) {

            appendToCSV(TRACES_CSV_FILE,new String[]{deviceId, venueId, event, other, timestamp+""});

            TempTracesDataSource tempTracesDataSource = new TempTracesDataSource(context.getApplicationContext());
            tempTracesDataSource.open();
            if(context.isOnline())
            {
                List<Trace> traces = tempTracesDataSource.getTempTraces();

                JSONArray allTracesJSArray = new JSONArray();

                for(int i = 0; i< traces.size(); i++)
                {
                    Trace trace = traces.get(i);
                    JSONObject json = new JSONObject();
                    try
                    {
                        json.put("device_id", trace.getDevice_id());
                        json.put("tag_1", trace.getVenue_id());
                        json.put("tag_2", trace.getEvent());
                        json.put("tag_3", trace.getOther());
                        json.put("timestamp", trace.getTimestamp());

                        allTracesJSArray.put(json);
                    }
                    catch(Exception e)
                    {
                        Log.e(HomeScreen.LOG_TAG, "JSON could not be created");
                        Log.e(HomeScreen.LOG_TAG,e.getMessage());
                        Log.e(HomeScreen.LOG_TAG,"Error: Storing Trace for the future");
                        tempTracesDataSource.addTrace(new Trace(deviceId, venueId, event, other, timestamp));
                        return null;
                    }
                }

                JSONObject json = new JSONObject();
                try
                {
                    json.put("device_id", deviceId);
                    json.put("tag_1", venueId);
                    json.put("tag_2", event);
                    json.put("tag_3", other);
                    json.put("timestamp", timestamp);
                    allTracesJSArray.put(json);
                }
                catch(Exception e)
                {
                    Log.e(HomeScreen.LOG_TAG, "JSON could not be created");
                    Log.e(HomeScreen.LOG_TAG,e.getMessage());
                    Log.e(HomeScreen.LOG_TAG,"Error: Storing Trace for the future");
                    tempTracesDataSource.addTrace(new Trace(deviceId, venueId, event, other, timestamp));
                    return null;
                }

                String URL = POST_TRACE_URL;

                try
                {
                    Log.d(HomeScreen.LOG_TAG, allTracesJSArray.toString());

                    RequestBody body = RequestBody.create(JSON, allTracesJSArray.toString());
                    Request request = new Request.Builder().url(URL).post(body).build();
                    OkHttpClient client = new OkHttpClient();
                    Response response = client.newCall(request).execute();

                    if (response.code() == 200) {
                        Log.d(HomeScreen.LOG_TAG,"Uploaded");
                        Log.d(HomeScreen.LOG_TAG, "Cleaning DB");
                        tempTracesDataSource.cleanDB();
                        return null;
                    }
                    else if(response.code() == 201) {
                        Log.d(HomeScreen.LOG_TAG,"Uploaded but wait time not updated");
                        Log.d(HomeScreen.LOG_TAG, "Cleaning DB");
                        tempTracesDataSource.cleanDB();
                        return null;
                    }
                    else
                    {
                        Log.d(HomeScreen.LOG_TAG,"Error uploading");
                        Log.e(HomeScreen.LOG_TAG,"Error: Storing Trace for the future");
                        tempTracesDataSource.addTrace(new Trace(deviceId, venueId, event, other, timestamp));
                        return null;
                    }

                }
                catch(Exception e)
                {
                    Log.e(HomeScreen.LOG_TAG, "HttpPost failed");
                    Log.e(HomeScreen.LOG_TAG,e.getMessage());
                    Log.e(HomeScreen.LOG_TAG,"Error: Storing Trace for the future");
                    tempTracesDataSource.addTrace(new Trace(deviceId, venueId, event, other, timestamp));
                    return null;
                }

            }
            else
            {
                Log.d(HomeScreen.LOG_TAG,"No internet connection. Storing");
                tempTracesDataSource.addTrace(new Trace(deviceId, venueId, event, other, timestamp));
                return null;
            }
        }

    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static void appendToCSV(String sFileName, String[] line)
    {
        try
        {
            File root = new File(Environment.getExternalStorageDirectory(),
                    APP_FOLDER);
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile, true);
            if(line.length>0)
            {
                writer.append(line[0]);
                for(int i = 1; i<line.length;i++)
                {
                    writer.append(",");
                    writer.append(line[i]);
                }
                writer.append("\n");
            }

            writer.flush();
            writer.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }


}

package com.aware.plugin.queue_estimation.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.aware.plugin.queue_estimation.R;
import com.aware.plugin.queue_estimation.db.LocationDataSource;
import com.aware.plugin.queue_estimation.db.PlacesDataSource;
import com.aware.plugin.queue_estimation.model.MyDBPlace;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by researcher on 29/06/15.
 */
public class HeatMap extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {



    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    private static final float COORDINATE_OFFSET = 0.00002f;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private static final double MAX_VALUE_QUEUE = 40;
    private static final double MAX_VALUE_MILLISECONDS = 3600000;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private static final float ZOOM_THRESHOLD = 15;

    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private GoogleApiClient mGoogleApiClient;

    private static final double MAX_DISTANCE = 25;

    private ArrayList<MyDBPlace> allPlaces;
    private ArrayList<Marker> allMarkers;

    private GoogleMap map;

    private HeatmapTileProvider heatmapTileProvider;
    private TileOverlay tileOverlay;
    private ArrayList<WeightedLatLng> latLngList;

    private boolean busy;

    private HashMap<String, Marker> markerHashMap;

    private HashMap<String,Object[]> waitTimes;
    private HashMap<Marker, MyDBPlace> markerPlaceHash;
    private HashMap<String, Marker> locationMarkerHash;

    private LocationManager locationManager;

    private Context context;

    private OkHttpClient client;

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new OkHttpClient();
        context = this;
        busy = false;
        waitTimes = new HashMap<>();
        markerPlaceHash = new HashMap<>();
        allMarkers = new ArrayList<>();
        markerHashMap = new HashMap<>();
        locationMarkerHash = new HashMap<>();
        mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        setContentView(R.layout.activity_heat_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        allPlaces = new ArrayList<>();
        latLngList = new ArrayList<>();

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                //.addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        ((ImageButton)findViewById(R.id.map_info_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle("Map Information")
                        .setMessage("The map displays a heatmap where red represents longer waiting lines" +
                                ", green short waiting lines and no color if we don't have information.\n\n" +
                                "Zoom in to view the markers for each place. The marker color indicates how long is the waiting time " +
                                "and the color of the base of the marker indicates how new is the information (green: recent, red: not recent," +
                                " gray: very old)\n\n" +
                                "Click on a marker to see the name and expected waiting time.")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(R.drawable.ic_chat_black_48dp)
                        .show();
            }
        });

        reloadMap();
    }

    @Override
    public void onMapReady(GoogleMap map) {

        map.setOnCameraChangeListener(this);
        map.setMyLocationEnabled(true);
        this.map = map;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.d(HomeScreen.LOG_TAG, "Place Id: " + markerPlaceHash.get(marker).getId());
                getWaitTimeOneRequest(markerPlaceHash.get(marker));
                return false;
            }
        });

    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if(!busy)
        {
            getNewPlaces(cameraPosition.target);
        }
        if(cameraPosition.zoom<=ZOOM_THRESHOLD)
        {
            if(allMarkers!=null && allMarkers.size()>0 )
            {
                if(allMarkers.get(0).isVisible())
                {
                    for(int i = 0; i<allMarkers.size();i++)
                    {
                        allMarkers.get(i).setVisible(false);
                    }
                }

            }
        }
        else
        {
            if(allMarkers!=null && allMarkers.size()>0 )
            {
                if(!allMarkers.get(0).isVisible())
                {
                    for(int i = 0; i<allMarkers.size();i++)
                    {
                        allMarkers.get(i).setVisible(true);
                    }
                }

            }
        }

    }

    private void getNewPlaces(LatLng latLng)
    {
        if(!locationSearched(latLng))
        {
            Log.d(HomeScreen.LOG_TAG, "Not searched");
            if(mGoogleApiClient.isConnected())
            {
                guessCurrentPlace(latLng);
            }
        }
        else
            Log.d(HomeScreen.LOG_TAG,"Searched");

    }

    private void store(MyDBPlace newPlace) {
        PlacesDataSource placesDataSource = new PlacesDataSource(getApplicationContext());
        placesDataSource.open();
        placesDataSource.addPlace(newPlace);
    }

    private void store(LatLng latLng) {
        LocationDataSource locationDataSource = new LocationDataSource(getApplicationContext());
        locationDataSource.open();
        locationDataSource.addSearchLocation(latLng);
    }

    private boolean locationSearched(LatLng latLng)
    {
        boolean result;
        LocationDataSource locationDataSource = new LocationDataSource(getApplicationContext());
        locationDataSource.open();
        result = locationDataSource.locationSearched(latLng,MAX_DISTANCE);
        return result;
    }

    private void reloadMap()
    {
        Log.d(HomeScreen.LOG_TAG, busy + "");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                allPlaces.clear();
                PlacesDataSource placesDataSource = new PlacesDataSource(getApplicationContext());
                placesDataSource.open();
                allPlaces.addAll(placesDataSource.getAllPlaces());
                getWaitTimeRequest();
//                new WaitTimeTask().execute();
            }
        };
        new Thread(runnable).start();
    }

    private void addHeatMap()
    {
        latLngList.clear();

        // Get the data: latitude/longitude positions of police stations.
        for(int i = 0; i < allPlaces.size()&& waitTimes.size() > 0;i++)
        {
            MyDBPlace place = allPlaces.get(i);
            place.setWaitTime((Double)waitTimes.get(place.getId())[0]);
            place.setLast_update((long)waitTimes.get(place.getId())[1]);
            latLngList.add(new WeightedLatLng(place.getLatLng(), place.getWaitTime()));
            if(!markerHashMap.containsKey(place.getId()))
            {
                String snippet;
                if(place.getWaitTime()>0)
                    snippet = "Waiting time: "+place.getWaitTime()+"min";
                else
                    snippet = "Waiting time: Unknown";
                LatLng newLocation = getNewLocation(place.getLatLng().latitude,place.getLatLng().longitude);
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(newLocation)
                        .title(place.getName())
                        .snippet(snippet)
                        .visible(map.getCameraPosition().zoom > ZOOM_THRESHOLD)
                        .icon(BitmapDescriptorFactory.fromBitmap(createMarker(place)))
                        .anchor(0.5f, 0.9f));
                place.setLatLng(newLocation);
                allMarkers.add(marker);
                markerHashMap.put(place.getId(), marker);
                markerPlaceHash.put(marker,place);
                locationMarkerHash.put(newLocation.latitude+","+newLocation.longitude,marker);
            }
            else
            {
                String snippet;
                if(place.getWaitTime()>0)
                    snippet = "Waiting time: "+place.getWaitTime()+"min";
                else
                    snippet = "Waiting time: Unknown";
                markerHashMap.get(place.getId()).setSnippet(snippet);
            }
        }

        if(latLngList.size()>0)
        {
            // Create a heat map tile provider, passing it the latlngs of the police stations.
            heatmapTileProvider = new HeatmapTileProvider.Builder()
                    .weightedData(latLngList)
                    .build();
            // Add a tile overlay to the map, using the heat map tile provider.
            tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));
        }
    }

    private void refreshHeatMap() {

        busy = false;

        if(heatmapTileProvider==null)
        {
            addHeatMap();
        }
        else
        {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    latLngList.clear();

                    // Get the data: latitude/longitude positions of police stations.
                    for(int i = 0; i<allPlaces.size();i++)
                    {
                        MyDBPlace place = allPlaces.get(i);
                        place.setWaitTime((Double)waitTimes.get(place.getId())[0]);
                        place.setLast_update((long)waitTimes.get(place.getId())[1]);
                        latLngList.add(new WeightedLatLng(place.getLatLng(),place.getWaitTime()));
                    }

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if(latLngList.size()>0)
                            {
                                heatmapTileProvider.setWeightedData(latLngList);
                                tileOverlay.clearTileCache();
                                // Create a heat map tile provider, passing it the latlngs of the police stations.
                                for(int i = 0; i<allPlaces.size();i++)
                                {
                                    MyDBPlace place = allPlaces.get(i);
                                    if(!markerHashMap.containsKey(place.getId()))
                                    {
                                        String snippet;
                                        if(place.getWaitTime()>0)
                                            snippet = "Waiting time: "+place.getWaitTime()+"min";
                                        else
                                            snippet = "Waiting time: Unknown";
                                        LatLng newLocation = getNewLocation(place.getLatLng().latitude,place.getLatLng().longitude);
                                        Marker marker = map.addMarker(new MarkerOptions()
                                                .position(newLocation)
                                                .title(place.getName())
                                                .snippet(snippet)
                                                .visible(map.getCameraPosition().zoom > ZOOM_THRESHOLD)
                                                .icon(BitmapDescriptorFactory.fromBitmap(createMarker(place)))
                                                .anchor(0.5f, 0.9f));
                                        place.setLatLng(newLocation);
                                        allMarkers.add(marker);
                                        markerHashMap.put(place.getId(), marker);
                                        markerPlaceHash.put(marker,place);
                                        locationMarkerHash.put(newLocation.latitude+","+newLocation.longitude,marker);
                                    }
                                    else
                                    {
                                        String snippet;
                                        if(place.getWaitTime()>0)
                                            snippet = "Waiting time: "+place.getWaitTime()+"min";
                                        else
                                            snippet = "Waiting time: Unknown";
                                        markerHashMap.get(place.getId()).setSnippet(snippet);
                                    }
                                }
                            }
                        }
                    });
                }
            };
            new Thread(runnable).start();

        }
    }

    private void refreshMarker(MyDBPlace parameterPlace) {

        int index = allPlaces.indexOf(parameterPlace);
        if(index>=0)
        {
            allPlaces.get(index).setWaitTime((Double)waitTimes.get(allPlaces.get(index).getId())[0]);
            allPlaces.get(index).setLast_update((long)waitTimes.get(allPlaces.get(index).getId())[1]);
        }
        String snippet;
        if(parameterPlace.getWaitTime()>0)
            snippet = "Waiting time: "+parameterPlace.getWaitTime()+"min";
        else
            snippet = "Waiting time: Unknown";

        boolean shown = false;
        if(markerHashMap.get(parameterPlace.getId()).isInfoWindowShown())
            shown = true;

        markerHashMap.get(parameterPlace.getId()).remove();
        allMarkers.remove(markerHashMap.get(parameterPlace.getId()));
        markerPlaceHash.remove(markerHashMap.get(parameterPlace.getId()));
        markerHashMap.remove(parameterPlace.getId());

        Marker marker = map.addMarker(new MarkerOptions()
                .position(parameterPlace.getLatLng())
                .title(parameterPlace.getName())
                .snippet(snippet)
                .visible(map.getCameraPosition().zoom > ZOOM_THRESHOLD)
                .icon(BitmapDescriptorFactory.fromBitmap(createMarker(parameterPlace)))
                .anchor(0.5f, 0.9f));
        allMarkers.add(marker);
        markerHashMap.put(parameterPlace.getId(), marker);
        markerPlaceHash.put(marker,parameterPlace);
        locationMarkerHash.put(parameterPlace.getLatLng().latitude+","+parameterPlace.getLatLng().longitude,marker);

        if(shown)
        {
            marker.showInfoWindow();
        }
//        markerHashMap.get(parameterPlace.getId()).setSnippet(snippet);
//        markerHashMap.get(parameterPlace.getId()).setIcon(BitmapDescriptorFactory.fromBitmap(createMarker(parameterPlace)));
//        if(markerHashMap.get(parameterPlace.getId()).isInfoWindowShown())
//            markerHashMap.get(parameterPlace.getId()).showInfoWindow();
    }

    private LatLng getNewLocation(double latitude, double longitude)
    {
        if(locationMarkerHash.containsKey(latitude+","+longitude))
        {
            latitude = latitude + 1 * COORDINATE_OFFSET;
            longitude = longitude + 1 * COORDINATE_OFFSET;
            return getNewLocation(latitude, longitude);
        }
        return new LatLng(latitude,longitude);
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {  // more about this later
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        //Do stuff
        //guessCurrentPlace(null);
        Log.d(HomeScreen.LOG_TAG, "connected to GoogleAPI");

    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.d(HomeScreen.LOG_TAG, "connection suspended");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(HomeScreen.LOG_TAG, "connection failed");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            Log.d(HomeScreen.LOG_TAG,"Error code" +connectionResult.getErrorCode());
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
//        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
//        dialogFragment.setArguments(args);
//        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }


    private void guessCurrentPlace(LatLng latLng) {
        String[] keywords = {"bakery", "bar", "cafe", "night_club", "restaurant", "ravintola"};
//        getPlacesRequest(latLng, keywords);
        new UpdatePlacesTask(latLng, keywords).execute();
    }

    private Bitmap createMarker(MyDBPlace place)
    {
        double waitTime = place.getWaitTime();
        float timeSinceUpdate = ((float) System.currentTimeMillis() - (float) place.getLast_update());
        if(timeSinceUpdate>259200000)
            timeSinceUpdate = -1;
        Bitmap bottom_circle = drawableToBitmap(getResources().getDrawable(R.drawable.bottom_circle));
        Paint bottom_circle_paint = new Paint();
        ColorFilter bottom_circle_filter = new LightingColorFilter(getColor(timeSinceUpdate, MAX_VALUE_MILLISECONDS), 0);
        bottom_circle_paint.setColorFilter(bottom_circle_filter);

        Bitmap marker = BitmapFactory.decodeResource(getResources(), R.drawable.map_marker_hi2);

        Paint marker_paint = new Paint();
        ColorFilter marker_filter = new LightingColorFilter(getColor(waitTime, MAX_VALUE_QUEUE), 0);
        marker_paint.setColorFilter(marker_filter);

        Bitmap big = Bitmap.createBitmap(marker.getWidth(), bottom_circle.getHeight() / 2 + marker.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(big);
        canvas.drawBitmap(bottom_circle, (marker.getWidth() - bottom_circle.getWidth()) / 2, marker.getHeight() - bottom_circle.getHeight() / 2, bottom_circle_paint);
        canvas.drawBitmap(marker, 0, 0, marker_paint);

        return big;
    }

    private static Bitmap drawableToBitmap (Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static int getColor(double value, double max) {
        double theValue = 1 - Math.max(Math.min(value/max,1),0);
        float color[] = new float[3];
        if((value == 0 && max == MAX_VALUE_QUEUE)||value == -1)
        {
            return Color.LTGRAY;
        }
        else
        {
            color[0] = ((float) (theValue * 0.3))*360; // Hue (note 0.4 = Green, see huge chart below)
            color[1] = (float) 0.9; // Saturation
            color[2] = (float) 0.9; // Brightness
            return Color.HSVToColor(color);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if(map!=null)
        {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
            map.animateCamera(cameraUpdate);
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class UpdatePlacesTask extends AsyncTask<Void, Void, Boolean>
    {

        private LatLng latLng;
        private String[] keyword;

        public UpdatePlacesTask(LatLng latLng, String[] keyword)
        {
            Log.d(HomeScreen.LOG_TAG,busy+"");
            this.latLng = latLng;
            this.keyword = keyword;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            busy = true;
            Log.d(HomeScreen.LOG_TAG, busy + "");

            boolean answer = true;
            String result[] = new String[keyword.length*3];
            String jsonResponse = null;

            String theURL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";

            theURL += "key=AIzaSyDeS7X-VKQjeKHb4o2mBUSDQZjCx9GflIo";
            theURL += "&location="+latLng.latitude+","+latLng.longitude;
            theURL += "&rankby=distance";
            int index = 0;
            for(int s = 0; s<keyword.length;s++)
            {
                String URL = theURL + "&keyword="+keyword[s];
                Log.d(HomeScreen.LOG_TAG,URL);

                Request request = new Request.Builder().url(URL).build();

                try
                {
                    Response response = client.newCall(request).execute();

                    if (response.code() == 200) {
                        String response_body = response.body().string();
                        jsonResponse = response_body;
                        result[index] = jsonResponse;
                        index++;
                        JSONObject jObject = new JSONObject(jsonResponse);
                        try
                        {
                            Log.d(HomeScreen.LOG_TAG,"4");
                            String token = jObject.getString("next_page_token");
                            String newURL = URL + "&pagetoken="+token;
                            request = new Request.Builder().url(newURL).build();
                            response = client.newCall(request).execute();

                            if (response.code()== 200) {
                                Log.d(HomeScreen.LOG_TAG,"8");
                                Log.d(HomeScreen.LOG_TAG, "9");
                                response_body = response.body().string();
                                Log.v(HomeScreen.LOG_TAG, "Your data: " + response_body);
                                jsonResponse = response_body.split("divider")[0];
                                result[index] = jsonResponse;
                                index++;
                                jObject = new JSONObject(jsonResponse);
                                try
                                {
                                    Log.d(HomeScreen.LOG_TAG,"10");
                                    token = jObject.getString("next_page_token");
                                    newURL = URL + "&pagetoken="+token;

                                    Log.d(HomeScreen.LOG_TAG, "11");
                                    Thread.sleep(500);
                                    Log.d(HomeScreen.LOG_TAG, "12");
                                    request = new Request.Builder().url(newURL).build();
                                    response = client.newCall(request).execute();
                                    Log.d(HomeScreen.LOG_TAG,"13");

                                    if (response.code() == 200) {
                                        Log.d(HomeScreen.LOG_TAG,"14");
                                        Log.d(HomeScreen.LOG_TAG, "15");
                                        response_body = response.body().string();
                                        Log.v(HomeScreen.LOG_TAG, "Your data: " + response_body);
                                        jsonResponse = response_body.split("divider")[0];
                                        result[index] = jsonResponse;
                                        index++;

                                    } else {
                                        Log.e(HomeScreen.LOG_TAG, "Failed to get 41-60 places");
                                    }
                                }
                                catch (Exception e)
                                {
                                    Log.e(HomeScreen.LOG_TAG,e.toString());
                                    Log.d(HomeScreen.LOG_TAG, "16");
                                }


                            } else {
                                Log.e(HomeScreen.LOG_TAG, "Failed to get 21-40 places");
                            }
                        }
                        catch (Exception e)
                        {
                            Log.d(HomeScreen.LOG_TAG,e.toString());
                            Log.d(HomeScreen.LOG_TAG,"17");
                        }


                    } else {
                        Log.e(HomeScreen.LOG_TAG, "Failed to get 1-20 places");
                        answer = false;
                        break;
                    }
                }
                catch(Exception ex)
                {
                    answer = false;
                    Log.e(HomeScreen.LOG_TAG, "Failed"); //response data
                    Log.e(HomeScreen.LOG_TAG,ex.toString());
                    break;
                }
            }


            JSONObject jObject;
            JSONArray jsonArray;

            for(int i = 0; i<result.length;i++)
            {
                Log.d(HomeScreen.LOG_TAG,"18");
                if(result[i]!=null)
                {
                    try
                    {
                        jObject = new JSONObject(result[i]);
                        jsonArray = jObject.getJSONArray("results");

                        for(int j = 0; j<jsonArray.length();j++) {
                            Log.d(HomeScreen.LOG_TAG,"19");
                            JSONObject jsonPlace = jsonArray.getJSONObject(j);
                            String place_id = jsonPlace.getString("place_id");
                            String name = jsonPlace.getString("name");
                            JSONArray typesArray = jsonPlace.getJSONArray("types");
                            Log.d(name,typesArray.toString());
                            if(typesArray.toString().toLowerCase().contains("bakery")||
                                    typesArray.toString().toLowerCase().contains("bar")||
                                    typesArray.toString().toLowerCase().contains("cafe")||
                                    typesArray.toString().toLowerCase().contains("night_club")||
                                    typesArray.toString().toLowerCase().contains("restaurant")||
                                    name.toLowerCase().contains("restaurant")||
                                    name.toLowerCase().contains("ravintola"))
                            {
                                double latitude = jsonPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                                double longitude = jsonPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lng");

                                MyDBPlace place = new MyDBPlace(name,new LatLng(latitude,longitude),place_id);
                                store(place);
                            }
                        }
                    }
                    catch(Exception e)
                    {
                        Log.e(HomeScreen.LOG_TAG, "Failed"); //response data
                        answer = false;
                        return answer;
                    }
                }
            }

            return answer;
        }

        @Override
        protected void onPostExecute(Boolean answer) {
            if(answer)
            {
                store(latLng);
                reloadMap();
            }
            else
            {
                busy = false;
            }
            Log.d(HomeScreen.LOG_TAG, busy + "");

        }
    }

    private void getWaitTimeRequest()
    {


        try
        {
            JSONArray jsonArray = new JSONArray();
            for(int i = 0; i<allPlaces.size();i++)
            {
                jsonArray.put(allPlaces.get(i).getId());
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("places",jsonArray);
            Log.d(HomeScreen.LOG_TAG, jsonObject.toString());
            String URL = "http://pan0166.panoulu.net/queue_estimation/get_wait_times.php";
            RequestBody body = RequestBody.create(JSON, jsonObject.toString());
            Request request = new Request.Builder().url(URL).post(body).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.d(HomeScreen.LOG_TAG,"Can't get wait times");
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if (response.code() == 200) {
                        String response_body = response.body().string();
                        Log.v(HomeScreen.LOG_TAG, "Your data: " + response_body);
                        String jsonResponse = response_body;
                        JSONArray jArray = null;
                        try {
                            jArray = new JSONArray(jsonResponse);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(jArray!=null)
                        {
                            HashMap<String,Object[]> result = new HashMap<>();

                            for(int i = 0; i<jArray.length();i++)
                            {
                                try {
                                    if(jArray.getJSONObject(i).getString("place_id")!=null)
                                    {
                                        result.put(jArray.getJSONObject(i).getString("place_id"),
                                                new Object[] {Double.parseDouble(jArray.getJSONObject(i).getString("wait_time")),
                                                        Long.parseLong(jArray.getJSONObject(i).getString("last_update"))}
                                        );
                                    }
                                } catch (JSONException e)
                                {
                                    e.printStackTrace();
                                    result = null;
                                    break;
                                }

                            }
                            int data[] = new int[] {1,4,5};
                            Log.d(HomeScreen.LOG_TAG, result.toString());

                            final HashMap<String,Object[]> result_waitTimes = result;

                            new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (result_waitTimes != null) {
                                        waitTimes = result_waitTimes;
                                    }
                                    refreshHeatMap();
                                }
                            });


                        }

                    }
                }
            });


        }
        catch (Exception e)
        {
            Log.d(HomeScreen.LOG_TAG,e.toString());
        }

    }

    private void getWaitTimeOneRequest(MyDBPlace... params)
    {
        final MyDBPlace place;




        MyDBPlace placeToGet = params[0];
        place = placeToGet;
        try
        {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(placeToGet.getId());

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("places",jsonArray);
            Log.d(HomeScreen.LOG_TAG, jsonObject.toString());
            String URL = "http://pan0166.panoulu.net/queue_estimation/get_wait_times.php";

            RequestBody body = RequestBody.create(JSON, jsonObject.toString());

            Request request = new Request.Builder().url(URL).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.d(HomeScreen.LOG_TAG,"Error geting waittimeone");
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if (response.code() == 200) {
                        JSONArray jArray = null;
                        String response_body = response.body().string();
                        Log.v(HomeScreen.LOG_TAG, "Your data: " + response_body);
                        String jsonResponse = response_body;
                        try {
                            jArray = new JSONArray(jsonResponse);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(jArray!= null)
                        {
                            ArrayList<Object> result = new ArrayList<>();
                            try {
                                for(int i = 0; i<jArray.length();i++)
                                {

                                    if(jArray.getJSONObject(i).getString("place_id")!=null)
                                    {
                                        result.add(jArray.getJSONObject(i).getString("place_id"));
                                        result.add(Double.parseDouble(jArray.getJSONObject(i).getString("wait_time")));
                                        result.add(Long.parseLong(jArray.getJSONObject(i).getString("last_update")));
                                    }


                                }
                                Log.d(HomeScreen.LOG_TAG,result.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                result = null;
                            }

                            final ArrayList<Object> objectArrayList = result;

                            new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if(objectArrayList != null)
                                    {
                                        waitTimes.put((String) objectArrayList.get(0),new Object[] {(Double) objectArrayList.get(1),(Long)objectArrayList.get(2)});
                                        place.setWaitTime((Double) objectArrayList.get(1));
                                        place.setLast_update((Long)objectArrayList.get(2));
                                        refreshMarker(place);
                                    }
                                }
                            });

                        }

//                        new WaitTimeOneTask(place, jArray).execute();

                    }
                }
            });

        }
        catch (Exception e)
        {
            Log.d(HomeScreen.LOG_TAG,e.toString());
        }
    }

}

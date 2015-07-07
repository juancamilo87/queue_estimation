package com.aware.plugin.tracescollector.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.SyncStateContract;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.aware.plugin.tracescollector.R;
import com.aware.plugin.tracescollector.db.LocationDataSource;
import com.aware.plugin.tracescollector.db.PlacesDataSource;
import com.aware.plugin.tracescollector.model.MyDBPlace;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by researcher on 29/06/15.
 */
public class HeatMap extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {



    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private static final float ZOOM_THRESHOLD = 15;

    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private GoogleApiClient mGoogleApiClient;

    private static final double MAX_DISTANCE = 50;
    private static final double MAX_RADIUS = 50;

    private ArrayList<MyDBPlace> allPlaces;
    private ArrayList<Marker> allMarkers;

    private GoogleMap map;

    private HeatmapTileProvider heatmapTileProvider;
    private TileOverlay tileOverlay;
    private ArrayList<WeightedLatLng> latLngList;

    private boolean busy;

    private HashMap<String, Marker> markerHashMap;

    private HashMap<String,Integer> waitTimes;
    private HashMap<Marker, MyDBPlace> markerPlaceHash;

    private LocationManager locationManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        busy = false;
        waitTimes = new HashMap<>();
        markerPlaceHash = new HashMap<>();
        allMarkers = new ArrayList<>();
        markerHashMap = new HashMap<>();
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

        ((ImageButton)findViewById(R.id.test_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO test
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
                Log.d("Marker","Place Id: "+markerPlaceHash.get(marker).getId());
                new WaitTimeOneTask().execute(markerPlaceHash.get(marker));
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
            Log.d("Search", "Not searched");
            if(mGoogleApiClient.isConnected())
            {
                guessCurrentPlace(latLng);
            }
        }
        else
            Log.d("Search","Searched");

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
        Log.d("BooleanM", busy + "");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                allPlaces.clear();
                PlacesDataSource placesDataSource = new PlacesDataSource(getApplicationContext());
                placesDataSource.open();
                allPlaces.addAll(placesDataSource.getAllPlaces());

                new WaitTimeTask().execute();
            }
        };
        new Thread(runnable).start();
    }

    private void addHeatMap()
    {
        latLngList.clear();

        // Get the data: latitude/longitude positions of police stations.
        for(int i = 0; i<allPlaces.size();i++)
        {
            MyDBPlace place = allPlaces.get(i);
            place.setWaitTime(waitTimes.get(place.getId()));
            latLngList.add(new WeightedLatLng(place.getLatLng(), place.getWaitTime()));
            if(!markerHashMap.containsKey(place.getId()))
            {
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(place.getLatLng())
                        .title(place.getName())
                        .snippet("Waiting time: "+place.getWaitTime()+"min")
                        .visible(map.getCameraPosition().zoom>ZOOM_THRESHOLD));
                allMarkers.add(marker);
                markerHashMap.put(place.getId(), marker);
                markerPlaceHash.put(marker,place);
            }
            else
            {
                markerHashMap.get(place.getId()).setSnippet("Waiting time: "+place.getWaitTime()+"min");
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
                        place.setWaitTime(waitTimes.get(place.getId()));
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
                                        Marker marker = map.addMarker(new MarkerOptions()
                                                .position(place.getLatLng())
                                                .title(place.getName())
                                                .snippet("Waiting time: "+place.getWaitTime()+"min")
                                                .visible(map.getCameraPosition().zoom>ZOOM_THRESHOLD));
                                        allMarkers.add(marker);
                                        markerHashMap.put(place.getId(),marker);
                                        markerPlaceHash.put(marker,place);
                                    }
                                    else
                                    {
                                        markerHashMap.get(place.getId()).setSnippet("Waiting time: "+place.getWaitTime()+"min");
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
            allPlaces.get(index).setWaitTime(waitTimes.get(allPlaces.get(index).getId()));
        }

        markerHashMap.get(parameterPlace.getId()).setSnippet("Waiting time: "+parameterPlace.getWaitTime()+"min");
        markerHashMap.get(parameterPlace.getId()).showInfoWindow();
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
        Log.d("tracescollector", "connected to GoogleAPI");

    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.d("tracescollector", "connection suspended");
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
        Log.d("tracescollector", "connection failed");
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
            Log.d("tracescollector","Error code" +connectionResult.getErrorCode());
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

        new GetPlacesTask(latLng).execute();


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

    private class GetPlacesTask extends AsyncTask<Void, Void, Boolean>
    {

        private LatLng latLng;

        public GetPlacesTask(LatLng latLng)
        {
            busy = true;
            Log.d("Boolean",busy+"");
            this.latLng = latLng;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean answer = true;
            String result[] = new String[3];
            String jsonResponse = null;

            HttpClient client = new DefaultHttpClient();
            String URL = "https://maps.googleapis.com/maps/api/place/search/json?";

            URL += "key=AIzaSyDeS7X-VKQjeKHb4o2mBUSDQZjCx9GflIo";
            URL += "&location="+latLng.latitude+","+latLng.longitude;
            URL += "&radius="+MAX_RADIUS;
            URL += "&keyword=";
            URL += "bakery";
            URL += "%7C";
            URL += "bar";
            URL += "%7C";
            URL += "cafe";
            URL += "%7C";
            URL += "night_club";
            URL += "%7C";
            URL += "restaurant";
            URL += "%7C";
            URL += "ravintola";
            Log.d("URL",URL);
            try
            {
                Log.d("Task","0");
                // Create Request to server and get response
                StringBuilder builder = new StringBuilder();
                Log.d("Task","0.1");
                HttpGet httpGet = new HttpGet(URL);
                Log.d("Task","0.2");
                HttpResponse response = client.execute(httpGet);
                Log.d("Task","1");
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    Log.d("Task","2");
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    Log.d("Task","3");
                    Log.v("Getter", "Your data: " + builder.toString());
                    jsonResponse = builder.toString();
                    result[0] = jsonResponse;
                    JSONObject jObject = new JSONObject(jsonResponse);
                    try
                    {
                        Log.d("Task","4");
                        String token = jObject.getString("next_page_token");
                        String newURL = URL + "&pagetoken="+token;
                        httpGet = new HttpGet(newURL);
                        Log.d("Task","5");
                        Thread.sleep(500);
                        Log.d("Task","6");
                        response = client.execute(httpGet);
                        Log.d("Task","7");
                        statusLine = response.getStatusLine();
                        statusCode = statusLine.getStatusCode();
                        if (statusCode == 200) {
                            Log.d("Task","8");
                            entity = response.getEntity();
                            content = entity.getContent();
                            reader = new BufferedReader(
                                    new InputStreamReader(content));
                            builder = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                                builder.append(line);
                            }
                            Log.d("Task","9");
                            Log.v("Getter", "Your data: " + builder.toString());
                            jsonResponse = builder.toString().split("divider")[0];
                            result[1] = jsonResponse;
                            jObject = new JSONObject(jsonResponse);
                            try
                            {
                                Log.d("Task","10");
                                token = jObject.getString("next_page_token");
                                newURL = URL + "&pagetoken="+token;
                                httpGet = new HttpGet(newURL);
                                Log.d("Task","11");
                                Thread.sleep(500);
                                Log.d("Task", "12");
                                response = client.execute(httpGet);
                                Log.d("Task","13");
                                statusLine = response.getStatusLine();
                                statusCode = statusLine.getStatusCode();
                                if (statusCode == 200) {
                                    Log.d("Task","14");
                                    entity = response.getEntity();
                                    content = entity.getContent();
                                    reader = new BufferedReader(
                                            new InputStreamReader(content));
                                    builder = new StringBuilder();
                                    while ((line = reader.readLine()) != null) {
                                        builder.append(line);
                                    }
                                    Log.d("Task","15");
                                    Log.v("Getter", "Your data: " + builder.toString());
                                    jsonResponse = builder.toString().split("divider")[0];
                                    result[2] = jsonResponse;

                                } else {
                                    Log.e("Getter", "Failed to get 41-60 places");
                                }
                            }
                            catch (Exception e)
                            {
                                Log.e("Task",e.toString());
                                Log.d("Task", "16");
                            }


                        } else {
                            Log.e("Getter", "Failed to get 21-40 places");
                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("Task",e.toString());
                        Log.d("Task","17");
                    }


                } else {
                    Log.e("Getter", "Failed to get 1-20 places");
                    answer = false;
                    return answer;
                }
            }
            catch(Exception ex)
            {
                answer = false;
                Log.e("Getter", "Failed"); //response data
                Log.e("Getter",ex.toString());
                return answer;
            }


            JSONObject jObject;
            JSONArray jsonArray;

            for(int i = 0; i<result.length;i++)
            {
                Log.d("Task","18");
                if(result[i]!=null)
                {
                    try
                    {
                        jObject = new JSONObject(result[i]);
                        jsonArray = jObject.getJSONArray("results");

                        for(int j = 0; j<jsonArray.length();j++) {
                            Log.d("Task","19");
                            JSONObject jsonPlace = jsonArray.getJSONObject(j);
                            String place_id = jsonPlace.getString("place_id");
                            String name = jsonPlace.getString("name");
                            double latitude = jsonPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                            double longitude = jsonPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lng");

                            MyDBPlace place = new MyDBPlace(name,new LatLng(latitude,longitude),place_id);
                            store(place);
                        }
                    }
                    catch(Exception e)
                    {
                        Log.e("JSON", "Failed"); //response data
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
            Log.d("BooleanP",busy+"");

        }
    }

    private void test() {
        Log.d("T","Start");
        int[] colors = {
                Color.rgb(102, 0, 255), // green
                Color.rgb(255, 0, 0)    // red
        };
        float[] startPoints = {
                0.2f, 1f
        };
        Gradient gradient = new Gradient(colors, startPoints);
        // Create the tile provider.
        if (latLngList.size() > 0) {
            Log.d("T","More than 0");
            if (heatmapTileProvider != null) {
                Log.d("T", "not null");
                heatmapTileProvider.setWeightedData(latLngList);
                tileOverlay.clearTileCache();

            } else {
                Log.d("T", "null");
                heatmapTileProvider = new HeatmapTileProvider.Builder()
                        .weightedData(latLngList)
                        .gradient(gradient)
                        .build();
                heatmapTileProvider.setRadius(50);
                tileOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));
                Log.d("Is Null", "True");
            }
        }
    }

    private class WaitTimeTask extends AsyncTask<Void, Void, HashMap<String,Integer>>
    {

        @Override
        protected HashMap<String,Integer> doInBackground(Void... params) {
            HashMap<String,Integer> result = new HashMap<>();
            try
            {
                JSONArray jsonArray = new JSONArray();
                for(int i = 0; i<allPlaces.size();i++)
                {
                    jsonArray.put(allPlaces.get(i).getId());
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("places",jsonArray);
                Log.d("JSON", jsonObject.toString());
                HttpClient client = new DefaultHttpClient();
                String URL = "http://pan0166.panoulu.net/queue_estimation/get_wait_times.php";

                HttpPost httpPost = new HttpPost(URL);

                AbstractHttpEntity abstractHttpEntity = new ByteArrayEntity(jsonObject.toString().getBytes("UTF8"));
                abstractHttpEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setEntity(abstractHttpEntity);

                HttpResponse response = client.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(content));
                    String line;
                    StringBuilder builder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    Log.v("Getter", "Your data: " + builder.toString());
                    String jsonResponse = builder.toString();
                    JSONArray jArray = new JSONArray(jsonResponse);

                    for(int i = 0; i<jArray.length();i++)
                    {
                        if(jArray.getJSONObject(i).getString("place_id")!=null)
                        {
                            result.put(jArray.getJSONObject(i).getString("place_id"),
                                    Integer.parseInt(jArray.getJSONObject(i).getString("wait_time")));
                        }

                    }
                    Log.d("Hash",result.toString());
                }

            }
            catch (Exception e)
            {
                Log.d("Error",e.toString());
                return null;
            }

            return result;
        }

        @Override
        protected void onPostExecute(HashMap<String, Integer> stringIntegerHashMap) {
            if(stringIntegerHashMap != null)
            {
                waitTimes = stringIntegerHashMap;
            }
            refreshHeatMap();
        }
    }

    private class WaitTimeOneTask extends AsyncTask<MyDBPlace, Void, ArrayList<Object>>
    {

        MyDBPlace place;

        @Override
        protected ArrayList<Object> doInBackground(MyDBPlace... params) {
            ArrayList<Object> result = new ArrayList<>();
            MyDBPlace placeToGet = params[0];
            place = placeToGet;
            try
            {
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(placeToGet.getId());

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("places",jsonArray);
                Log.d("JSON", jsonObject.toString());
                HttpClient client = new DefaultHttpClient();
                String URL = "http://pan0166.panoulu.net/queue_estimation/get_wait_times.php";

                HttpPost httpPost = new HttpPost(URL);

                AbstractHttpEntity abstractHttpEntity = new ByteArrayEntity(jsonObject.toString().getBytes("UTF8"));
                abstractHttpEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setEntity(abstractHttpEntity);

                HttpResponse response = client.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(content));
                    String line;
                    StringBuilder builder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    Log.v("Getter", "Your data: " + builder.toString());
                    String jsonResponse = builder.toString();
                    JSONArray jArray = new JSONArray(jsonResponse);

                    for(int i = 0; i<jArray.length();i++)
                    {
                        if(jArray.getJSONObject(i).getString("place_id")!=null)
                        {
                            result.add(jArray.getJSONObject(i).getString("place_id"));
                            result.add(Integer.parseInt(jArray.getJSONObject(i).getString("wait_time")));
                        }

                    }
                    Log.d("Array",result.toString());
                }

            }
            catch (Exception e)
            {
                Log.d("Error",e.toString());
                return null;
            }

            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<Object> objectArrayList) {
            if(objectArrayList != null)
            {
                waitTimes.put((String) objectArrayList.get(0),(Integer) objectArrayList.get(1));
                place.setWaitTime((Integer)objectArrayList.get(1));
                refreshMarker(place);
            }
        }
    }

}

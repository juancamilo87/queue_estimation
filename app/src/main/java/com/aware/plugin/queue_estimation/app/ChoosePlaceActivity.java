package com.aware.plugin.queue_estimation.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.PlaceReport;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.aware.plugin.queue_estimation.R;

/**
 * Created by researcher on 02/06/15.
 */
public class ChoosePlaceActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Context context;

    private GoogleApiClient mGoogleApiClient;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private ListView listView;

    private ArrayAdapter<MyPlace> listAdapter;

    private ArrayList<MyPlace> places;

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new OkHttpClient();
        context = this;
        mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        setContentView(R.layout.activity_choose_place);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                //.addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        listView = (ListView) findViewById(R.id.choose_list);
        places = new ArrayList<MyPlace>();
        listAdapter = new ArrayAdapter<MyPlace>(this,android.R.layout.simple_list_item_1,places);
        listView.setAdapter(listAdapter);
        listView.setEmptyView(findViewById(R.id.no_places));
        findViewById(R.id.no_places).setVisibility(View.GONE);
        findViewById(R.id.empty_place).setVisibility(View.VISIBLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final MyPlace item = (MyPlace) parent.getItemAtPosition(position);
                Places.PlaceDetectionApi.reportDeviceAtPlace(mGoogleApiClient, PlaceReport.create(item.getPlace().getId(), "Queuing"))
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status status) {
                                Log.i(HomeScreen.LOG_TAG, "Report place result result: " + status.toString());
                            }
                        });
                Intent returnIntent = new Intent();
                returnIntent.putExtra("venue", (Parcelable) item.getPlace());
                setResult(RESULT_OK, returnIntent);
                finish();
            }

        });
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
        Log.d(HomeScreen.LOG_TAG,"connected to GoogleAPI");
        guessCurrentPlace();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.d(HomeScreen.LOG_TAG,"connection suspended");
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
        Log.d(HomeScreen.LOG_TAG,"connection failed");
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

    /* A fragment to display an error dialog */
//    public static class ErrorDialogFragment extends DialogFragment {
//        public ErrorDialogFragment() { }
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//            // Get the error code and retrieve the appropriate dialog
//            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
//            return GooglePlayServicesUtil.getErrorDialog(errorCode,
//                    this.getActivity(), REQUEST_RESOLVE_ERROR);
//        }
//
//        @Override
//        public void onDismiss(DialogInterface dialog) {
//            ((ChoosePlaceActivity)getActivity()).onDialogDismissed();
//        }
//    }

    private void guessCurrentPlace() {
        LatLng latLng;
        Location gps_location = ((LocationManager) this.getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location network_location = ((LocationManager) this.getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(gps_location == null)
            latLng = new LatLng(network_location.getLatitude(),network_location.getLongitude());
        else if(network_location == null)
            latLng = new LatLng(gps_location.getLatitude(),gps_location.getLongitude());
        else
        {
            long time_delta = Math.abs(gps_location.getTime()-network_location.getTime());
            if(time_delta<60000)
            {
                if(gps_location.getAccuracy()<network_location.getAccuracy())
                {
                    latLng = new LatLng(gps_location.getLatitude(),gps_location.getLongitude());
                }
                else
                    latLng = new LatLng(network_location.getLatitude(),network_location.getLongitude());
            }
            else
            {
                if(gps_location.getTime()>network_location.getTime())
                {
                    latLng = new LatLng(gps_location.getLatitude(),gps_location.getLongitude());
                }
                else
                    latLng = new LatLng(network_location.getLatitude(),network_location.getLongitude());
            }

        }
        Log.d(HomeScreen.LOG_TAG, "latlng = " + latLng.toString());
        String[] keywords = {"bakery", "bar", "cafe", "night_club", "restaurant", "ravintola"};
        new UpdatePlacesTask(latLng, keywords).execute();

//        getPlacesRequest(latLng, keywords);

//        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace( mGoogleApiClient, null );
//        result.setResultCallback( new ResultCallback<PlaceLikelihoodBuffer>() {
//            @Override
//            public void onResult( PlaceLikelihoodBuffer likelyPlaces ) {
//                findViewById(R.id.empty_place).setVisibility(View.GONE);
//                Log.d("tracescollector","Got result with " + likelyPlaces.getCount() + " likely places");
//                //PlaceLikelihood placeLikelihood = likelyPlaces.get( 0 );
//                PlaceLikelihood newPlace;
//                listAdapter.clear();
//                places.clear();
//                for(int i = 0; i< likelyPlaces.getCount(); i++)
//                {
//                    newPlace = likelyPlaces.get(i);
//                    List<Integer> placeTypes = newPlace.getPlace().getPlaceTypes();
//                    if(placeTypes.contains(Place.TYPE_BAKERY)||
//                            placeTypes.contains(Place.TYPE_BAR)||
//                            placeTypes.contains(Place.TYPE_CAFE)||
//                            placeTypes.contains(Place.TYPE_NIGHT_CLUB)||
//                            placeTypes.contains(Place.TYPE_RESTAURANT)||
//                            newPlace.getPlace().getName().toString().toLowerCase().contains("ravintola")||
//                            newPlace.getPlace().getName().toString().toLowerCase().contains("restaurant"))
//                    {
//                        places.add(new MyPlace(newPlace.getPlace()));
//                    }
//                }
//                if(places.size() == 0)
//                {
//                    findViewById(R.id.no_places).setVisibility(View.VISIBLE);
//                }
//                else
//                {
//                    places.add(new MyPlace(new OtherPlace()));
//                }
//                listAdapter.notifyDataSetChanged();
//                likelyPlaces.release();
//            }
//        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }


    private class UpdatePlacesTask extends AsyncTask<Void, Void, Void>
    {

        private LatLng latLng;
        private String[] keyword;

        public UpdatePlacesTask(LatLng latLng, String[] keyword)
        {
            this.latLng = latLng;
            this.keyword = keyword;
        }

        @Override
        protected Void doInBackground(Void... params) {

            places.clear();
            String result[] = new String[keyword.length*3];
            String jsonResponse = null;

            String theURL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";

            theURL += "key=AIzaSyDeS7X-VKQjeKHb4o2mBUSDQZjCx9GflIo";
            theURL += "&location="+latLng.latitude+","+latLng.longitude;
            theURL += "&rankby=distance";

            for(int s = 0; s<keyword.length;s++)
            {
                String URL = theURL + "&keyword="+keyword[s];
                Log.d(HomeScreen.LOG_TAG,URL);
                Request request = new Request.Builder().url(URL).build();
                try {
                    Response response = client.newCall(request).execute();
                    if(response.code() == 200)
                    {
                        String response_body = response.body().string();
                        Log.v(HomeScreen.LOG_TAG, "Your data: " + response_body);
                        jsonResponse = response_body;
                        result[s] = jsonResponse;
                    }
                    else
                    {
                        Log.e(HomeScreen.LOG_TAG, "Failed to get 1-20 places");
                    }
                } catch (IOException e) {
                    Log.e(HomeScreen.LOG_TAG, "Failed"); //response data
                    Log.e(HomeScreen.LOG_TAG, e.toString());
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
                            Log.d(HomeScreen.LOG_TAG,typesArray.toString());
                            if(typesArray.toString().toLowerCase().contains("bakery")||
                                    typesArray.toString().toLowerCase().contains("bar")||
                                    typesArray.toString().toLowerCase().contains("cafe")||
                                    typesArray.toString().toLowerCase().contains("night_club")||
                                    typesArray.toString().toLowerCase().contains("restaurant")||
                                    name.toLowerCase().contains("restaurant")||
                                    name.toLowerCase().contains("ravintola"))
                            {
                                MyPlace placeToAdd = new MyPlace(new OtherPlace(name,
                                        place_id,
                                        SphericalUtil.computeDistanceBetween(
                                                latLng,
                                                new LatLng(
                                                        jsonPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lat"),
                                                        jsonPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lng"))
                                        )));
                                if(!places.contains(placeToAdd))
                                    places.add(placeToAdd);
                            }
                        }
                    }
                    catch(Exception e)
                    {
                        Log.e(HomeScreen.LOG_TAG, "Failed"); //response data
                    }
                }
            }

            Collections.sort(places, new LocationComparator());
            double current_distance = 10000;
            for(int i = places.size()-1; i>18 || current_distance>200; i--)
            {
                current_distance = places.get(i).getDistance();
                places.remove(i);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
//            if(answer)
//            {
            findViewById(R.id.empty_place).setVisibility(View.GONE);

            if(places.size() == 0)
            {
                findViewById(R.id.no_places).setVisibility(View.VISIBLE);
            }
            else
            {
                places.add(new MyPlace(new OtherPlace()));
            }
            listAdapter.notifyDataSetChanged();
//            }
//            else
//            {
//                busy = false;
//            }
//            Log.d("BooleanP",busy+"");

        }
    }

    public class LocationComparator implements Comparator<MyPlace>
    {
        public int compare(MyPlace left, MyPlace right) {
            if(left.getDistance()<right.getDistance())
            {
                return -1;
            }
            else if(left.getDistance()==right.getDistance())
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }
    }




}

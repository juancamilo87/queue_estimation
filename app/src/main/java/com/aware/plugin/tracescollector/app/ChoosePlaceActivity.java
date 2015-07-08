package com.aware.plugin.tracescollector.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.aware.plugin.tracescollector.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.PlaceReport;
import com.google.android.gms.location.places.Places;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                                Log.i("Queuing", "Report place result result: " + status.toString());
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
        Log.d("tracescollector","connected to GoogleAPI");
        guessCurrentPlace();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.d("tracescollector","connection suspended");
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
        Log.d("tracescollector","connection failed");
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
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace( mGoogleApiClient, null );
        result.setResultCallback( new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult( PlaceLikelihoodBuffer likelyPlaces ) {
                findViewById(R.id.empty_place).setVisibility(View.GONE);
                Log.d("tracescollector","Got result with " + likelyPlaces.getCount() + " likely places");
                //PlaceLikelihood placeLikelihood = likelyPlaces.get( 0 );
                PlaceLikelihood newPlace;
                listAdapter.clear();
                places.clear();
                for(int i = 0; i< likelyPlaces.getCount(); i++)
                {
                    newPlace = likelyPlaces.get(i);
                    List<Integer> placeTypes = newPlace.getPlace().getPlaceTypes();
                    if(placeTypes.contains(Place.TYPE_BAKERY)||
                            placeTypes.contains(Place.TYPE_BAR)||
                            placeTypes.contains(Place.TYPE_CAFE)||
                            placeTypes.contains(Place.TYPE_NIGHT_CLUB)||
                            placeTypes.contains(Place.TYPE_RESTAURANT)||
                            newPlace.getPlace().getName().toString().toLowerCase().contains("ravintola")||
                            newPlace.getPlace().getName().toString().toLowerCase().contains("ravintolat"))
                    {
                        places.add(new MyPlace(newPlace.getPlace()));
                    }
                }
                if(places.size() == 0)
                {
                    findViewById(R.id.no_places).setVisibility(View.VISIBLE);
                }
                listAdapter.notifyDataSetChanged();
                likelyPlaces.release();
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }




}

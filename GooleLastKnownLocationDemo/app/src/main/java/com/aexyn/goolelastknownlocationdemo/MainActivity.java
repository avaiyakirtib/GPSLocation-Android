package com.aexyn.goolelastknownlocationdemo;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

/**
 * Demonstrate the use of Google Location Api to know the last known location of device
 * <p>
 * First of all we need to create a project at google console using this projects name and package name
 * Reference :- http://stackoverflow.com/questions/34367870/where-do-i-get-a-google-services-json
 * After successfully registered google will provide the link for GoogleServices.json file
 * Download it and copy/paste it to the app folder of the project
 * <p>
 * Then Go to the manifest file and write two lines code
 * <meta-data
 * android:name="com.google.android.gms.version"
 * android:value="@integer/google_play_services_version" />
 * <p>
 * Before using the google's location, we need to initialize the GoogleApiClient.
 * see code below method name is - setupGoogleApiClient()
 * <p>
 * Then we need to implement these three interfaces given by the google
 * GoogleApiClient.ConnectionCallbacks,                         // Will perform when Google's connection is successful or not
 * GoogleApiClient.OnConnectionFailedListener,                  // Will perform when google's connection is not successful
 * LocationListener,                                            // Will return device's location
 * ResultCallback<LocationSettingsResult>                       // To check if gps is on or off - powered by google itself
 * <p>
 * onLocationChanged(Location location)  // This method will return location of the device
 * <p>
 * put all the code written here as it is to the desired class when needed
 * GoogleApiClient need Minimum api is 15 (Just for info)
 */

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ResultCallback<LocationSettingsResult> {

    /* Google Api */
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected LocationSettingsRequest mLocationSettingsRequest;
    protected Location mCurrentLocation;
    protected Boolean mRequestingLocationUpdates = false;
    boolean isRequestingPermission = false;
    public static final int REQUEST_CHECK_SETTINGS = 0x2;
    public static final int PERMISSION_REQUEST_CODE_START_UPDATES = 1;
    public static final int PERMISSION_REQUEST_CODE_INITIALIZE = 2;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 30 * 1000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    Double longitude;
    Double latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBuildingLocationApi();
        setupGoogleApiClient();

    }

    private void setupGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void startBuildingLocationApi() {
        if (!checkPermission()) {
            requestPermissionLocation(PERMISSION_REQUEST_CODE_INITIALIZE);
            return;
        }
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();
        checkLocationSettings();
        mGoogleApiClient.connect();
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    protected void checkLocationSettings() {
        com.google.android.gms.common.api.PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    protected void startLocationUpdates() {

        try {
            if (!checkPermission()) {
                if (!isRequestingPermission) {
                    requestPermissionLocation(PERMISSION_REQUEST_CODE_START_UPDATES);
                }
            } else {
                LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient,
                        mLocationRequest,
                        this
                ).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        mRequestingLocationUpdates = true;
                    }
                });
            }
        } catch (Exception e) {
            Log.e("Gofer", "StartLocation Exception --- > " + e.getMessage());
        }
    }

    private void requestPermissionLocation(int permissionCode) {
        isRequestingPermission = true;
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, permissionCode);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCurrentLocation == null) {
            if (!checkPermission()) {
                if (!isRequestingPermission)
                    requestPermissionLocation(PERMISSION_REQUEST_CODE_INITIALIZE);
            } else startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("Gofer", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("Gofer", "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.e("Gofer", "All location settings are satisfied.");
                mGoogleApiClient.connect();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.e("Gofer", "Location settings are not satisfied. Show the user a dialog to" + "upgrade location settings ");
                try {
                    status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.e("Gofer", "Location settings are inadequate, and cannot be fixed here. Dialog " + "not created.");
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_INITIALIZE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Grant Permission for use your location", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

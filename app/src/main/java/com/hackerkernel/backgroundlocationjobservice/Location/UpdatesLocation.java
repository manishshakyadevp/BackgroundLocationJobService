package com.hackerkernel.backgroundlocationjobservice.Location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class UpdatesLocation {
    private static final String TAG = UpdatesLocation.class.getSimpleName();
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5 * 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    //      Callback for location.
    private LocationCallback mLocationCallback;

    //    Fused Location Provider API
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;

    //Current location.
    private Location mLocation;

    private ILocationProvider iLocationProvider;

    public UpdatesLocation(ILocationProvider iLocationProvider) {
        this.iLocationProvider = iLocationProvider;
    }

    public void onCreate(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                latestLocation(locationResult.getLastLocation());
            }
        };
        createLocationRequest();
        getLastLocation();
    }

    public void onStart() {
        requestLocationUpdates();
    }

    public void onStop() {
        removeLocationUpdates();
    }

    public void requestLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    mLocationCallback, Looper.getMainLooper());
        } catch (SecurityException exp) {
            Log.d(TAG, "requestLocationUpdatesException: " + exp);
        }
    }

    public void removeLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(mLocationCallback);
        } catch (SecurityException exp) {
            Log.d(TAG, "removeLocationUpdatesException: " + exp);
        }
    }

    private void getLastLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                                Log.i(TAG, "getLastLocation " + mLocation);
                                latestLocation(mLocation);
                            } else {
                                Log.w(TAG, "Failed to find location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "no location permission." + unlikely);
        }
    }

    private void latestLocation(Location location) {
        Log.i(TAG, "latestLocation: " + location);
        mLocation = location;
        if (this.iLocationProvider != null) {
            this.iLocationProvider.onLocationUpdate(mLocation);
        }
    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public interface ILocationProvider {
        void onLocationUpdate(Location location);
    }
}
// LocationService.java

package com.example.persona;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.*;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    public static final String ACTION_LOCATION_BROADCAST = "com.example.persona.LOCATION_BROADCAST";
    public static final String EXTRA_LOCATION = "extra_location";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseHelper firebaseHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService started");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseHelper = new FirebaseHelper();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult == null){
                    return;
                }
                Location location = locationResult.getLastLocation();
                // Send location update
                Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
                intent.putExtra(EXTRA_LOCATION, location);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                // Save location to Firebase
                firebaseHelper.saveUserLocation(location);
            }
        };

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000) // 10 seconds
                .setFastestInterval(5000) // 5 seconds
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted!");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}

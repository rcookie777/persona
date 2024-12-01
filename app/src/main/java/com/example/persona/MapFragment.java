// MapFragment.java

package com.example.persona;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.*;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private static final double EAST_LANSING_LAT = 42.7370;
    private static final double EAST_LANSING_LNG = -84.4839;
    private static final float TWO_MILES_IN_METERS = 3218.68f; // 2 miles in meters

    private FirebaseHelper firebaseHelper;
    private String currentUserUid;

    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                if (location != null) {
                    updateMapLocation(location);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper();
        currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(locationUpdateReceiver,
                new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(locationUpdateReceiver);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        // Do not enable My Location layer since it requires location permissions
        // No permissions are needed if we don't call methods that require them

        // Set custom info window adapter if needed
        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                // Use default info window
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Customize the contents if needed
                return null;
            }
        });

        map.setOnInfoWindowClickListener(marker -> {
        });

        map.setOnMarkerClickListener(marker -> {
            String uid = (String) marker.getTag();
            if (uid != null && !uid.equals(currentUserUid)) {
                // Start AIConversationActivity
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("otherUserUid", uid);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void updateMapLocation(Location location) {
        if (map == null) {
            return;
        }
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        map.clear();
        map.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));

        // Check if within 2 miles of East Lansing
        Location eastLansingLocation = new Location("");
        eastLansingLocation.setLatitude(EAST_LANSING_LAT);
        eastLansingLocation.setLongitude(EAST_LANSING_LNG);

        float distanceInMeters = location.distanceTo(eastLansingLocation);

        if (distanceInMeters <= TWO_MILES_IN_METERS) {
            Toast.makeText(requireContext(), "Hello from East Lansing", Toast.LENGTH_LONG).show();
        }

        // Fetch and display nearby users
        fetchNearbyUsers(location);
    }

    private void fetchNearbyUsers(Location location) {
        GeoPoint center = new GeoPoint(location.getLatitude(), location.getLongitude());
        double radiusInKm = 1.60934; // 1 mile in km

        firebaseHelper.getUsersWithinRadius(center, radiusInKm, new FirebaseHelper.UsersLocationCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documentSnapshots) {
                List<String> uidList = new ArrayList<>();
                Map<String, GeoPoint> userLocations = new HashMap<>();

                for (DocumentSnapshot document : documentSnapshots) {
                    String uid = document.getString("uid");
                    if (uid != null && !uid.equals(currentUserUid)) {
                        GeoPoint geoPoint = document.getGeoPoint("coordinates");
                        uidList.add(uid);
                        userLocations.put(uid, geoPoint);
                    }
                }

                if (uidList.isEmpty()) {
                    // No nearby users
                    return;
                }

                // Fetch user profiles
                firebaseHelper.getUserProfiles(uidList, new FirebaseHelper.UserProfilesCallback() {
                    @Override
                    public void onSuccess(QuerySnapshot profileSnapshot) {
                        for (DocumentSnapshot profileDoc : profileSnapshot.getDocuments()) {
                            String uid = profileDoc.getId();
                            // Get traits and personality
                            List<String> traits = (List<String>) profileDoc.get("traits");
                            List<String> personalityTraits = (List<String>) profileDoc.get("personalityTraits");

                            // Get location from userLocations map
                            GeoPoint geoPoint = userLocations.get(uid);
                            if (geoPoint != null) {
                                // Add marker to map
                                LatLng userLatLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                String title = "User";
                                String snippet = "Traits: " + traits + "\nPersonality: " + personalityTraits;
                                Marker marker = map.addMarker(new MarkerOptions()
                                        .position(userLatLng)
                                        .title(title)
                                        .snippet(snippet));
                                assert marker != null;
                                marker.setTag(uid);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(), "Failed to fetch user profiles: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to fetch nearby users: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

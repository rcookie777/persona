// FirebaseHelper.java

package com.example.persona;

import android.location.Location;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import org.imperiumlabs.geofirestore.GeoFirestore;
import org.imperiumlabs.geofirestore.listeners.GeoQueryDataEventListener;
import org.imperiumlabs.geofirestore.GeoQuery;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference geoFirestoreRef;
    private GeoFirestore geoFirestore;

    public FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        geoFirestoreRef = db.collection("user_locations");
        geoFirestore = new GeoFirestore(geoFirestoreRef);
    }

    // Sign in method
    public void signIn(String email, String password, final OnCompleteListener<AuthResult> listener) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    // Sign up method
    public void signUp(String email, String password, final OnCompleteListener<AuthResult> listener) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(listener);
    }

    // Get current user
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // Sign out method
    public void signOut() {
        mAuth.signOut();
    }

    // Save user profile data to Firestore
    public Task<Void> saveUserProfile(Map<String, Object> profileData) {
        String uid = mAuth.getCurrentUser().getUid();
        return db.collection("users").document(uid).set(profileData);
    }

    // Retrieve user profile data
    public Task<DocumentSnapshot> getUserProfile() {
        String uid = mAuth.getCurrentUser().getUid();
        return db.collection("users").document(uid).get();
    }

    public void saveUserLocation(Location location) {
        String uid = mAuth.getCurrentUser().getUid();
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        Map<String, Object> data = new HashMap<>();
        data.put("coordinates", geoPoint);
        data.put("uid", uid);
        // Save location to GeoFirestore
        geoFirestore.setLocation(uid, geoPoint);

        // Optionally, update timestamp or other data
        data.put("timestamp", FieldValue.serverTimestamp());
        geoFirestoreRef.document(uid).set(data, SetOptions.merge());
    }

    // Get users within a radius
    public void getUsersWithinRadius(GeoPoint center, double radiusInKm, final UsersLocationCallback callback) {
        GeoQuery geoQuery = geoFirestore.queryAtLocation(center, radiusInKm);

        geoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
            List<DocumentSnapshot> documentSnapshots = new ArrayList<>();

            @Override
            public void onDocumentEntered(DocumentSnapshot documentSnapshot, GeoPoint location) {
                documentSnapshots.add(documentSnapshot);
            }

            @Override
            public void onDocumentExited(DocumentSnapshot documentSnapshot) {
                // Not needed
            }

            @Override
            public void onDocumentMoved(DocumentSnapshot documentSnapshot, GeoPoint location) {
                // Not needed
            }

            @Override
            public void onDocumentChanged(DocumentSnapshot documentSnapshot, GeoPoint location) {
                // Not needed
            }

            @Override
            public void onGeoQueryReady() {
                callback.onSuccess(documentSnapshots);
            }

            @Override
            public void onGeoQueryError(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public interface UsersLocationCallback {
        void onSuccess(List<DocumentSnapshot> documentSnapshots);
        void onFailure(Exception e);
    }

    // Get user profiles for a list of UIDs
    public void getUserProfiles(List<String> uids, final UserProfilesCallback callback) {
        CollectionReference usersRef = db.collection("users");
        usersRef.whereIn(FieldPath.documentId(), uids)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    callback.onSuccess(querySnapshot);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e);
                });
    }

    public interface UserProfilesCallback {
        void onSuccess(QuerySnapshot querySnapshot);
        void onFailure(Exception e);
    }

}

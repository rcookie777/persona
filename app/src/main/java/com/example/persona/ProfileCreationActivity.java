// ProfileCreationActivity.java

package com.example.persona;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.provider.MediaStore;
import android.widget.*;
import android.view.View;
import android.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Base64;

public class ProfileCreationActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGE_SIZE = 800000; // Max 800 KB

    private ImageView avatarImageView;
    private TextView traitsTextView;
    private TextView personalityTextView;
    private Button saveButton;
    private ProgressBar progressBar;

    private Uri avatarImageUri;
    private Bitmap avatarBitmap;
    private FirebaseHelper firebaseHelper;
    private FirebaseAuth mAuth;

    // Arrays for traits and personality options
    private String[] traitsList = {"Brave", "Cautious", "Curious", "Friendly", "Honest", "Intelligent"};
    private boolean[] selectedTraits;
    private ArrayList<Integer> traitsSelectedItems = new ArrayList<>();

    private String[] personalityList = {"Introvert", "Extrovert", "Optimistic", "Pessimistic", "Ambitious", "Easy-going"};
    private boolean[] selectedPersonality;
    private ArrayList<Integer> personalitySelectedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_creation);

        avatarImageView = findViewById(R.id.avatarImageView);
        traitsTextView = findViewById(R.id.traitsTextView);
        personalityTextView = findViewById(R.id.personalityTextView);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);

        firebaseHelper = new FirebaseHelper();
        mAuth = FirebaseAuth.getInstance();

        selectedTraits = new boolean[traitsList.length];
        selectedPersonality = new boolean[personalityList.length];

        avatarImageView.setOnClickListener(v -> openImageChooser());
        traitsTextView.setOnClickListener(v -> showTraitsDialog());
        personalityTextView.setOnClickListener(v -> showPersonalityDialog());
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Avatar"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle the image chooser result
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            avatarImageUri = data.getData();
            try {
                avatarBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), avatarImageUri);
                avatarBitmap = getResizedBitmap(avatarBitmap, 500); // Resize bitmap to max 500 pixels
                avatarImageView.setImageBitmap(avatarBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to set avatar image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to resize the bitmap
    private Bitmap getResizedBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void showTraitsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Traits");
        builder.setMultiChoiceItems(traitsList, selectedTraits, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!traitsSelectedItems.contains(which)) {
                    traitsSelectedItems.add(which);
                }
            } else {
                traitsSelectedItems.remove(Integer.valueOf(which));
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            StringBuilder traits = new StringBuilder();
            for (int i = 0; i < traitsSelectedItems.size(); i++) {
                traits.append(traitsList[traitsSelectedItems.get(i)]);
                if (i != traitsSelectedItems.size() - 1) {
                    traits.append(", ");
                }
            }
            traitsTextView.setText(traits.toString());
        });

        builder.setNegativeButton("Cancel", null);

        builder.setNeutralButton("Clear All", (dialog, which) -> {
            for (int i = 0; i < selectedTraits.length; i++) {
                selectedTraits[i] = false;
            }
            traitsSelectedItems.clear();
            traitsTextView.setText("Select Traits");
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showPersonalityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Personality Traits");
        builder.setMultiChoiceItems(personalityList, selectedPersonality, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!personalitySelectedItems.contains(which)) {
                    personalitySelectedItems.add(which);
                }
            } else {
                personalitySelectedItems.remove(Integer.valueOf(which));
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            StringBuilder personalities = new StringBuilder();
            for (int i = 0; i < personalitySelectedItems.size(); i++) {
                personalities.append(personalityList[personalitySelectedItems.get(i)]);
                if (i != personalitySelectedItems.size() - 1) {
                    personalities.append(", ");
                }
            }
            personalityTextView.setText(personalities.toString());
        });

        builder.setNegativeButton("Cancel", null);

        builder.setNeutralButton("Clear All", (dialog, which) -> {
            for (int i = 0; i < selectedPersonality.length; i++) {
                selectedPersonality[i] = false;
            }
            personalitySelectedItems.clear();
            personalityTextView.setText("Select Personality Traits");
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveProfile() {
        if (avatarBitmap == null) {
            Toast.makeText(this, "Please select an avatar image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (traitsSelectedItems.isEmpty()) {
            Toast.makeText(this, "Please select at least one trait", Toast.LENGTH_SHORT).show();
            return;
        }

        if (personalitySelectedItems.isEmpty()) {
            Toast.makeText(this, "Please select at least one personality trait", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedTraitsList = new ArrayList<>();
        for (int index : traitsSelectedItems) {
            selectedTraitsList.add(traitsList[index]);
        }

        List<String> selectedPersonalityList = new ArrayList<>();
        for (int index : personalitySelectedItems) {
            selectedPersonalityList.add(personalityList[index]);
        }

        progressBar.setVisibility(View.VISIBLE);

        // Compress and encode the avatar image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        avatarBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] imageData = baos.toByteArray();

        // Check if the image size is within Firestore limits
        if (imageData.length > MAX_IMAGE_SIZE) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Avatar image is too large. Please select a smaller image.", Toast.LENGTH_LONG).show();
            return;
        }

        String avatarBase64 = Base64.encodeToString(imageData, Base64.DEFAULT);

        // Save profile data to Firestore
        HashMap<String, Object> profileData = new HashMap<>();
        profileData.put("traits", selectedTraitsList);
        profileData.put("personalityTraits", selectedPersonalityList);
        profileData.put("avatarImage", avatarBase64);

        firebaseHelper.saveUserProfile(profileData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileCreationActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();

                    // Redirect to MainActivity
                    Intent intent = new Intent(ProfileCreationActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileCreationActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

package com.example.persona;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ChatPrefs";

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button loginButton;
    private TextView textViewSignUp;

    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        try {
            // Clear SharedPreferences when the app restarts
            SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.apply();

            // Initialize FirebaseHelper
            firebaseHelper = new FirebaseHelper();

            // Initialize UI elements
            editTextUsername = findViewById(R.id.editTextUsername);
            editTextPassword = findViewById(R.id.editTextPassword);
            loginButton = findViewById(R.id.button);
            textViewSignUp = findViewById(R.id.textViewSignUp);

            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = editTextUsername.getText().toString().trim();
                    String password = editTextPassword.getText().toString().trim();

                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firebaseHelper.signIn(email, password, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Sign in fails
                                Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            textViewSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Start RegisterActivity
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
            });

        } catch (Exception e) {
            e.printStackTrace(); // Log the exception
        }
    }
}

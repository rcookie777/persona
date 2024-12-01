package com.example.persona;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.persona.models.Conversation;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileNameTextView;
    private ImageView profileAvatarImageView;
    private Conversation conversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileNameTextView = findViewById(R.id.profileName);
        profileAvatarImageView = findViewById(R.id.profileAvatar);

        conversation = (Conversation) getIntent().getSerializableExtra("conversation");

        if (conversation != null) {
            profileNameTextView.setText(conversation.getName());
            profileAvatarImageView.setImageResource(conversation.getAvatarResource());
        }
    }
}

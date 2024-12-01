package com.example.persona;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.persona.models.Message;
import com.example.persona.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private static final int AI_RESPONSE_DELAY_MS = 10000; // 10 seconds

    private TextView chatTitleTextView;
    private TextView chatContentTextView;
    private Button stopButton;
    private Button startButton;

    private UserProfile currentUserProfile;
    private UserProfile otherUserProfile;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String currentUserUid;
    private String otherUserUid;

    private CollectionReference conversationsRef;
    private ListenerRegistration conversationListener;

    private boolean isConversationActive = true;
    private boolean isCurrentUserTurn = false;

    private String conversationId;

    private static final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatTitleTextView = findViewById(R.id.chatTitle);
        chatContentTextView = findViewById(R.id.chatContent);
        stopButton = findViewById(R.id.stopButton);
        startButton = findViewById(R.id.startButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        conversationsRef = db.collection("conversations");

        currentUserUid = mAuth.getCurrentUser().getUid();
        otherUserUid = getIntent().getStringExtra("otherUserUid");

        Log.d(TAG, "Current user UID: " + currentUserUid);
        Log.d(TAG, "Other user UID: " + otherUserUid);

        retrieveUserProfiles();

        stopButton.setOnClickListener(view -> {
            isConversationActive = false;
            if (conversationListener != null) {
                conversationListener.remove();
            }
            Log.d(TAG, "Conversation stopped by user.");
            stopButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
            Toast.makeText(ChatActivity.this, "Conversation stopped", Toast.LENGTH_SHORT).show();
        });

        startButton.setOnClickListener(view -> {
            isConversationActive = true;
            Log.d(TAG, "Conversation restarted by user.");
            stopButton.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.GONE);
            startConversation();
        });
    }

    private void retrieveUserProfiles() {
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        firebaseHelper.getUserProfiles(Arrays.asList(currentUserUid, otherUserUid), new FirebaseHelper.UserProfilesCallback() {
            @Override
            public void onSuccess(QuerySnapshot querySnapshot) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    UserProfile profile = doc.toObject(UserProfile.class);
                    profile.setUid(doc.getId());
                    if (doc.getId().equals(currentUserUid)) {
                        currentUserProfile = profile;
                    } else if (doc.getId().equals(otherUserUid)) {
                        otherUserProfile = profile;
                    }
                }
                if (currentUserProfile != null && otherUserProfile != null) {
                    startConversation();
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to retrieve user profiles", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ChatActivity.this, "Failed to retrieve user profiles: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startConversation() {
        conversationId = generateConversationId(currentUserUid, otherUserUid);
        chatTitleTextView.setText("Chat with " + otherUserUid);

        // Ensure that the conversation document exists with participants field
        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("participants", Arrays.asList(currentUserUid, otherUserUid));

        conversationsRef.document(conversationId)
                .set(conversationData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Conversation document created or updated.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create or update conversation document: " + e.getMessage());
                });

        if (!isConversationActive) {
            Log.d(TAG, "Conversation is not active. Skipping listener setup.");
            return;
        }

        if (conversationListener != null) {
            conversationListener.remove();
        }

        conversationListener = conversationsRef.document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(ChatActivity.this, "Failed to listen for conversation updates: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        StringBuilder conversationBuilder = new StringBuilder();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Message message = doc.toObject(Message.class);
                            conversationBuilder.append(message.getSenderId()).append(": ").append(message.getMessageContent()).append("\n");
                        }
                        chatContentTextView.setText(conversationBuilder.toString());

                        if (isConversationActive) {
                            determineConversationTurn(querySnapshot);
                        }
                    }
                });
    }

    private void determineConversationTurn(QuerySnapshot querySnapshot) {
        if (querySnapshot.getDocuments().size() == 0) {
            isCurrentUserTurn = currentUserUid.compareTo(otherUserUid) < 0;
            if (isCurrentUserTurn) {
                aiSendMessage();
            }
        } else {
            DocumentSnapshot lastMessageDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
            Message lastMessage = lastMessageDoc.toObject(Message.class);
            if (lastMessage.getSenderId().equals(currentUserUid)) {
                isCurrentUserTurn = false;
            } else {
                isCurrentUserTurn = true;
                aiSendMessage();
            }
        }
    }

    private void aiSendMessage() {
        if (!isCurrentUserTurn || !isConversationActive) {
            return;
        }
        Log.d(TAG, "Delaying AI response by " + (AI_RESPONSE_DELAY_MS / 1000) + " seconds...");
        mainHandler.postDelayed(() -> buildAIPromptAndSendMessage(), AI_RESPONSE_DELAY_MS);
    }

    private void buildAIPromptAndSendMessage() {
        Log.d(TAG, "Building AI prompt and sending message...");
        conversationsRef.document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Message> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Message message = doc.toObject(Message.class);
                        messages.add(message);
                    }

                    String conversationHistory = buildConversationHistory(messages);

                    Log.d(TAG, "Conversation History:\n" + conversationHistory);

                    AIService aiService = new AIService(this);

                    aiService.sendMessage(conversationHistory, currentUserProfile, otherUserProfile, new AIService.AIResponseCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Message aiMessage = new Message(currentUserUid, response, System.currentTimeMillis());

                            // Ensure that the conversation document exists
                            Map<String, Object> conversationData = new HashMap<>();
                            conversationData.put("participants", Arrays.asList(currentUserUid, otherUserUid));

                            conversationsRef.document(conversationId)
                                    .set(conversationData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        // Now add the message
                                        conversationsRef.document(conversationId)
                                                .collection("messages")
                                                .add(aiMessage)
                                                .addOnSuccessListener(documentReference -> Log.d(TAG, "AI response saved."))
                                                .addOnFailureListener(e -> Log.e(TAG, "Failed to save AI response: " + e.getMessage()));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to create or update conversation document: " + e.getMessage());
                                    });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(ChatActivity.this, "AI Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to retrieve messages: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String buildConversationHistory(List<Message> messages) {
        StringBuilder conversationBuilder = new StringBuilder();
        for (Message msg : messages) {
            conversationBuilder.append(msg.getSenderId()).append(": ").append(msg.getMessageContent()).append("\n");
        }
        return conversationBuilder.toString();
    }

    private String generateConversationId(String uid1, String uid2) {
        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationListener != null) {
            conversationListener.remove();
        }
        isConversationActive = false;
    }
}

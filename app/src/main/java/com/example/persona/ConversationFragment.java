package com.example.persona;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.persona.adapters.ConversationAdapter;
import com.example.persona.models.Conversation;
import com.example.persona.models.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ConversationFragment extends Fragment {

    private RecyclerView conversationRecyclerView;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserUid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_conversation, container, false);

        conversationRecyclerView = view.findViewById(R.id.conversationRecyclerView);
        conversationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserUid = mAuth.getCurrentUser().getUid();

        conversationList = new ArrayList<>();
        conversationAdapter = new ConversationAdapter(conversationList, conversation -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("otherUserUid", conversation.getOtherUserUid());
            startActivity(intent);
        }, conversation -> {
            // Handle long click if needed
        });

        conversationRecyclerView.setAdapter(conversationAdapter);

        loadConversations();

        return view;
    }

    private void loadConversations() {
        CollectionReference conversationsRef = db.collection("conversations");

        conversationsRef.whereArrayContains("participants", currentUserUid)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Failed to load conversations: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        List<Conversation> tempConversationList = new ArrayList<>();
                        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String conversationId = doc.getId();
                            List<String> participants = (List<String>) doc.get("participants");
                            String otherUserUid = participants.get(0).equals(currentUserUid) ? participants.get(1) : participants.get(0);

                            Task<QuerySnapshot> messageTask = doc.getReference().collection("messages")
                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(messageSnapshot -> {
                                        String lastMessage = "";
                                        if (!messageSnapshot.isEmpty()) {
                                            Message message = messageSnapshot.getDocuments().get(0).toObject(Message.class);
                                            lastMessage = message.getMessageContent();
                                        }

                                        Conversation conversation = new Conversation(otherUserUid, lastMessage, R.drawable.user);
                                        tempConversationList.add(conversation);
                                    });

                            tasks.add(messageTask);
                        }

                        // Wait for all tasks to complete
                        Tasks.whenAllSuccess(tasks).addOnSuccessListener(result -> {
                            conversationList.clear();
                            conversationList.addAll(tempConversationList);
                            conversationAdapter.notifyDataSetChanged();
                        });
                    }
                });
    }
}

// AIService.java
package com.example.persona;

import android.content.Context;
import android.util.Log;

import com.example.persona.models.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIService {
    private static final String TAG = "AIService";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama3-8b-8192";

    private OkHttpClient client;
    private String apiKey;
    private ExecutorService executorService;

    public AIService(Context context) {
        client = new OkHttpClient();
        executorService = Executors.newSingleThreadExecutor();

        apiKey = BuildConfig.GROQ_API_KEY;
        Log.d(TAG, "API_KEY: " + apiKey);
    }

    public interface AIResponseCallback {
        void onSuccess(String response);
        void onFailure(String errorMessage);
    }

    // Modified method signature to accept both user profiles
    public void sendMessage(String conversationHistory, UserProfile currentUserProfile, UserProfile otherUserProfile, AIResponseCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Preparing request to AI service...");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("model", MODEL);

                // Build messages array
                JSONArray messagesArray = new JSONArray();

                // Build the traits and personality traits strings for both users
                String currentUserTraits = String.join(", ", currentUserProfile.getTraits());
                String currentUserPersonality = String.join(", ", currentUserProfile.getPersonalityTraits());

                String otherUserTraits = String.join(", ", otherUserProfile.getTraits());
                String otherUserPersonality = String.join(", ", otherUserProfile.getPersonalityTraits());

                // Construct the system prompt content
                String systemContent = "You are an AI assistant with the following traits: " + currentUserTraits +
                        ". Your personality traits are: " + currentUserPersonality + ".\n" +
                        "You are conversing with another AI assistant that has the following traits: " + otherUserTraits +
                        ". Their personality traits are: " + otherUserPersonality + ".\n" +
                        "Engage in a conversation with the other AI, responding appropriately based on your traits and personality. If you get stuck act as a human with the personality and traits you have. Be curious about the other human behind the Ai.";

                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemContent);
                messagesArray.put(systemMessage);

                // Log the system message
                Log.d(TAG, "System message: " + systemMessage.toString());

                // Split the conversation history into messages
                String[] conversationLines = conversationHistory.split("\n");
                for (String line : conversationLines) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(": ", 2);
                    if (parts.length < 2) continue;

                    String senderId = parts[0].trim();
                    String content = parts[1].trim();

                    JSONObject messageObject = new JSONObject();
                    if (senderId.equals(currentUserProfile.getUid())) {
                        messageObject.put("role", "assistant"); // Assistant is the current AI
                    } else {
                        messageObject.put("role", "user"); // Other AI is treated as user
                    }
                    messageObject.put("content", content);
                    messagesArray.put(messageObject);
                }

                // Log the messages array
                Log.d(TAG, "Messages array: " + messagesArray.toString());

                jsonBody.put("messages", messagesArray);

                // Log the full request body
                Log.d(TAG, "Request body: " + jsonBody.toString());

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(GROQ_API_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                // Log the request details
                Log.d(TAG, "Sending request to URL: " + GROQ_API_URL);
                Log.d(TAG, "Request headers: " + request.headers().toString());

                Response response = client.newCall(request).execute();

                // Log the response code and message
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response message: " + response.message());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    Log.e(TAG, "Request failed with code " + response.code() + ": " + errorBody);
                    callback.onFailure("Unexpected code " + response.code() + ": " + errorBody);
                } else {
                    String responseBody = response.body().string();

                    // Log the raw response body
                    Log.d(TAG, "Response body: " + responseBody);

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray choices = jsonResponse.getJSONArray("choices");

                    // Log the choices array
                    Log.d(TAG, "Choices: " + choices.toString());

                    if (choices.length() > 0) {
                        JSONObject choice = choices.getJSONObject(0);
                        JSONObject message = choice.getJSONObject("message");
                        String aiResponse = message.getString("content");

                        // Log the AI's response
                        Log.d(TAG, "AI response: " + aiResponse);

                        callback.onSuccess(aiResponse.trim());
                    } else {
                        Log.e(TAG, "No choices found in response");
                        callback.onFailure("No response from AI");
                    }
                }
            } catch (JSONException | IOException e) {
                Log.e(TAG, "Exception occurred: " + e.getMessage(), e);
                callback.onFailure(e.getMessage());
            }
        });
    }
}

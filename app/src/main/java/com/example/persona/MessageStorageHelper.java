// MessageStorageHelper.java
package com.example.persona;

import android.content.Context;

import com.example.persona.models.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class MessageStorageHelper {

    private static final String MESSAGES_DIR = "messages";

    private Context context;

    public MessageStorageHelper(Context context) {
        this.context = context;
    }

    public void saveMessage(String userId, Message message) {
        long timestamp = System.currentTimeMillis();
        String fileName = String.format("%d_chat.json", timestamp);

        File dir = new File(context.getFilesDir(), MESSAGES_DIR + File.separator + userId);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, fileName);

        Map<String, String> messageMap = new HashMap<>();
        messageMap.put(message.getSenderId(), message.getMessageContent());

        Gson gson = new Gson();
        String json = gson.toJson(messageMap);

        try {
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Message> loadMessages(String userId) {
        List<Message> messages = new ArrayList<>();

        File dir = new File(context.getFilesDir(), MESSAGES_DIR + File.separator + userId);
        if (dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null) {
                Gson gson = new Gson();

                for (File file : files) {
                    try {
                        FileReader reader = new FileReader(file);
                        Type type = new TypeToken<Map<String, String>>() {}.getType();
                        Map<String, String> messageMap = gson.fromJson(reader, type);
                        reader.close();

                        for (Map.Entry<String, String> entry : messageMap.entrySet()) {
                            long timestamp = extractTimestampFromFileName(file.getName());
                            Message message = new Message(entry.getKey(), entry.getValue(), timestamp);
                            messages.add(message);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Collections.sort(messages, new Comparator<Message>() {
                    @Override
                    public int compare(Message o1, Message o2) {
                        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
                    }
                });
            }
        }

        return messages;
    }

    private long extractTimestampFromFileName(String fileName) {
        // File name format: timestamp_chat.json
        try {
            String[] parts = fileName.split("_");
            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    // Method to get the most recent messages (e.g., last 20 messages)
    public List<Message> getRecentMessages(String userId, int limit) {
        List<Message> messages = loadMessages(userId);
        int size = messages.size();
        if (size > limit) {
            return messages.subList(size - limit, size);
        } else {
            return messages;
        }
    }

    // Method to delete messages older than 12 hours
    public void deleteOldMessages(String userId, long olderThanMillis) {
        File dir = new File(context.getFilesDir(), MESSAGES_DIR + File.separator + userId);
        if (dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null) {
                long now = System.currentTimeMillis();

                for (File file : files) {
                    long timestamp = extractTimestampFromFileName(file.getName());
                    if (now - timestamp > olderThanMillis) {
                        file.delete();
                    }
                }
            }
        }
    }
}

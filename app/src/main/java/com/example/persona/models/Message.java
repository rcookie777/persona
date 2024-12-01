// Message.java
package com.example.persona.models;

import java.io.Serializable;

public class Message implements Serializable {
    private String senderId;
    private String messageContent;
    private long timestamp;

    public Message() {}

    public Message(String senderId, String messageContent, long timestamp) {
        this.senderId = senderId;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

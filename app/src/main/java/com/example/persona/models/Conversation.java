package com.example.persona.models;

import java.io.Serializable;

public class Conversation implements Serializable {
    private String name;
    private String lastMessage;
    private int avatarResource;
    private String otherUserUid;

    public Conversation(String otherUserUid, String lastMessage, int avatarResource) {
        this.name = otherUserUid;
        this.lastMessage = lastMessage;
        this.avatarResource = avatarResource;
        this.otherUserUid = otherUserUid;
    }

    public String getName() {
        return name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public int getAvatarResource() {
        return avatarResource;
    }

    public String getOtherUserUid() {
        return otherUserUid;
    }
}

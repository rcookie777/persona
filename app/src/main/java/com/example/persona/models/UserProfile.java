// UserProfile.java
package com.example.persona.models;

import java.io.Serializable;
import java.util.List;

public class UserProfile implements Serializable {
    private String uid; // Add UID field
    private List<String> traits;
    private List<String> personalityTraits;
    private String avatarImage;

    public UserProfile() {}

    public UserProfile(String uid, List<String> traits, List<String> personalityTraits, String avatarImage) {
        this.uid = uid;
        this.traits = traits;
        this.personalityTraits = personalityTraits;
        this.avatarImage = avatarImage;
    }

    // Getter and Setter for UID
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    // Existing getters and setters
    public List<String> getTraits() {
        return traits;
    }

    public void setTraits(List<String> traits) {
        this.traits = traits;
    }

    public List<String> getPersonalityTraits() {
        return personalityTraits;
    }

    public void setPersonalityTraits(List<String> personalityTraits) {
        this.personalityTraits = personalityTraits;
    }

    public String getAvatarImage() {
        return avatarImage;
    }

    public void setAvatarImage(String avatarImage) {
        this.avatarImage = avatarImage;
    }
}

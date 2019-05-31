package com.example.wnsvy.kakaonotiread.Model;

import io.realm.RealmObject;

public class Users extends RealmObject {
    private String userId;
    private String message;
    private String timeStamp;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}

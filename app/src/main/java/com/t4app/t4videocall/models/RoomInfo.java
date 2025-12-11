package com.t4app.t4videocall.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RoomInfo {

    @SerializedName("name")
    private String name;

    @SerializedName("participants")
    private List<String> participants;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }
}

package com.t4app.t4videocall.models;

import com.google.gson.annotations.SerializedName;

public class RoomResponse {
    @SerializedName("ok")
    private boolean ok;

    @SerializedName("room")
    private RoomInfo room;

    @SerializedName("shareUrl")
    private String shareUrl;

    @SerializedName("error")
    private String error;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public RoomInfo getRoom() {
        return room;
    }

    public void setRoom(RoomInfo room) {
        this.room = room;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }
}

package com.t4app.videocalltest.viewmodel.manager;

public class CallSessionState {
    private String roomName;
    private String userName;

    public CallSessionState() {
    }

    public CallSessionState(String roomName, String userName) {
        this.roomName = roomName;
        this.userName = userName;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isEmpty() {
        return roomName == null || roomName.isEmpty()
                || userName == null || userName.isEmpty();
    }
}

package com.t4app.t4videocall.models;

public class Room {

    public enum State{
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        DISCONNECTED
    }

    private State state;
    private String roomName;
    private String userName;

    public Room() {
    }

    public Room(State state, String roomName) {
        this.roomName = roomName;
        this.state = state;
    }

    public Room(String roomName,String userName , State state) {
        this.roomName = roomName;
        this.userName = userName;
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
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
}

package com.t4app.videocalltest.models;

public class Room {

    public enum State{
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        DISCONNECTED
    }

    private State state;
    private String name;

    public Room(State state, String name) {
        this.state = state;
        this.name = name;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

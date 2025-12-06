package com.t4app.videocalltest.viewmodel;

public class VideoCallViewEvent {

    public static class Connected extends VideoCallViewEvent {
        private final String roomName;
        private final String userName;

        public Connected(String roomName, String userName) {
            this.roomName = roomName;
            this.userName = userName;
        }

        public String getRoomName() {
            return roomName;
        }

        public String getUserName() {
            return userName;
        }
    }

    public static class JoinRoom extends VideoCallViewEvent {
        private final String roomName;
        private final String userName;

        public JoinRoom(String roomName, String userName) {
            this.roomName = roomName;
            this.userName = userName;
        }

        public String getRoomName() {
            return roomName;
        }

        public String getUserName() {
            return userName;
        }
    }

    public static class Disconnect extends VideoCallViewEvent {
        private final String roomName;
        private final String userName;

        public Disconnect(String roomName, String userName) {
            this.roomName = roomName;
            this.userName = userName;
        }

        public String getRoomName() {
            return roomName;
        }

        public String getUserName() {
            return userName;
        }
    }
}

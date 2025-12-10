package com.t4app.videocalltest.events;

import android.media.AudioDeviceInfo;

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

    public static class CreateRoom extends VideoCallViewEvent {
        private final String roomName;
        private final String userName;

        public CreateRoom(String roomName, String userName) {
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

    public static class IncomingCall extends VideoCallViewEvent {
        private final String roomName;
        private final String userName;

        public IncomingCall(String roomName, String userName) {
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

    public static final class ToggleLocalAudio extends VideoCallViewEvent {
        public ToggleLocalAudio() {}
    }

    public static final class ToggleLocalVideo extends VideoCallViewEvent {
        public ToggleLocalVideo() {}
    }

    public static class ChangeAudioOutput extends VideoCallViewEvent {
        private final AudioDeviceInfo deviceSelected;

        public ChangeAudioOutput(AudioDeviceInfo deviceSelected) {
            this.deviceSelected = deviceSelected;
        }

        public AudioDeviceInfo getDeviceSelected() {
            return deviceSelected;
        }
    }


    public static class OnResume extends VideoCallViewEvent { }
    public static class OnPause extends VideoCallViewEvent { }
}

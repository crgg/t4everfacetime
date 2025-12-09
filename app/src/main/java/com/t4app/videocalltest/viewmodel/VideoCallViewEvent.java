package com.t4app.videocalltest.viewmodel;

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

    public static class ToggleLocalAudio extends VideoCallViewEvent {
        private final String userName;
        private final boolean micEnable;

        public ToggleLocalAudio(String userName, boolean micEnable) {
            this.userName = userName;
            this.micEnable = micEnable;
        }

        public String getUserName() {
            return userName;
        }

        public boolean isMicEnable() {
            return micEnable;
        }
    }

    public static class ToggleLocalVideo extends VideoCallViewEvent {
        private final String userName;
        private final boolean videoEnable;

        public ToggleLocalVideo(String userName, boolean videoEnable) {
            this.userName = userName;
            this.videoEnable = videoEnable;
        }

        public String getUserName() {
            return userName;
        }

        public boolean isVideoEnable() {
            return videoEnable;
        }
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

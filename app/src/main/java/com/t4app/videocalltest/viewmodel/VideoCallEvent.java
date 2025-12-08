package com.t4app.videocalltest.viewmodel;

import com.t4app.videocalltest.models.RoomInfo;

import org.webrtc.IceCandidate;
import org.webrtc.VideoTrack;

import java.util.List;

public class VideoCallEvent {

    public static class Connected extends VideoCallEvent {
        private final RoomInfo roomInfo;
        private final String name;
        private final boolean iAmCaller;
        private final boolean inRoom;

        public Connected(RoomInfo roomInfo, String name, boolean iAmCaller, boolean inRoom) {
            this.roomInfo = roomInfo;
            this.name = name;
            this.iAmCaller = iAmCaller;
            this.inRoom = inRoom;
        }

        public boolean isInRoom() {
            return inRoom;
        }

        public boolean isiAmCaller() {
            return iAmCaller;
        }

        public RoomInfo getRoomInfo() {
            return roomInfo;
        }

        public String getName() {
            return name;
        }
    }

    public static class CallerConnected extends VideoCallEvent { }

    public static class Disconnect extends VideoCallEvent { }

    public static class RemoteDisconnect extends VideoCallEvent { }

    public static class SendIceCandidate extends VideoCallEvent {
        private final IceCandidate iceCandidate;

        public SendIceCandidate(IceCandidate iceCandidate) {
            this.iceCandidate = iceCandidate;
        }

        public IceCandidate getIceCandidate() {
            return iceCandidate;
        }
    }

    public static class AddRemoteUser extends VideoCallEvent {
        private final VideoTrack remoteVideoTrack;
        private final boolean isLocalInRoom;
        public AddRemoteUser(VideoTrack remoteVideoTrack, boolean isLocalInRoom) {
            this.remoteVideoTrack = remoteVideoTrack;
            this.isLocalInRoom = isLocalInRoom;
        }

        public VideoTrack getRemoteVideoTrack() {
            return remoteVideoTrack;
        }

        public boolean isLocalInRoom() {
            return isLocalInRoom;
        }
    }

    public static class AddLocalUser extends VideoCallEvent {
       private final List<String> streamIds;
        public AddLocalUser(List<String> streamIds) {
           this.streamIds = streamIds;
        }

        public List<String> getStreamIds() {
            return streamIds;
        }
    }

    public static class ToggleLocalAudio extends VideoCallEvent {
        private final boolean audioStatus;
        public ToggleLocalAudio(boolean audioStatus) {
            this.audioStatus = audioStatus;
        }

        public boolean isAudioStatus() {
            return audioStatus;
        }
    }

    public static class ToggleLocalVideo extends VideoCallEvent {
        private final boolean videoStatus;
        public ToggleLocalVideo(boolean videoStatus) {
            this.videoStatus = videoStatus;
        }

        public boolean isVideoStatus() {
            return videoStatus;
        }
    }

    public static class ToggleRemoteAudio extends VideoCallEvent {
        private final boolean audioStatus;
        public ToggleRemoteAudio(boolean audioStatus) {
            this.audioStatus = audioStatus;
        }

        public boolean isAudioStatus() {
            return audioStatus;
        }
    }

    public static class ToggleRemoteVideo extends VideoCallEvent {
        private final boolean videoStatus;
        public ToggleRemoteVideo(boolean videoStatus) {
            this.videoStatus = videoStatus;
        }

        public boolean isVideoStatus() {
            return videoStatus;
        }
    }

}

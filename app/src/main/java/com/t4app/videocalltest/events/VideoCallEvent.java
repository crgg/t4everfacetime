package com.t4app.videocalltest.events;

import androidx.annotation.Nullable;

import com.t4app.videocalltest.models.RoomInfo;

import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.VideoTrack;

public class VideoCallEvent {

    public static final class Connecting extends VideoCallEvent {
        public static final Connecting INSTANCE = new Connecting();
        private Connecting() {}
    }

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

    public static class RoomConnected extends VideoCallEvent {
        private final String name;

        public RoomConnected(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static final class ConnectFailure extends VideoCallEvent {
        public static final ConnectFailure INSTANCE = new ConnectFailure();
        private ConnectFailure() {}
    }

    public static class Disconnect extends VideoCallEvent { }



    public static class UserJoined extends VideoCallEvent {
        private final String name;
        private final String room;

        public UserJoined(String name, String room) {
            this.name = name;
            this.room = room;
        }

        public String getName() {
            return name;
        }

        public String getRoom() {
            return room;
        }
    }

    public static class UserLeave extends VideoCallEvent {
        private final String name;
        private final String room;

        public UserLeave(String name, String room) {
            this.name = name;
            this.room = room;
        }

        public String getName() {
            return name;
        }

        public String getRoom() {
            return room;
        }
    }

    public static class CallerConnected extends VideoCallEvent { }


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

    public static abstract class LocalParticipantEvent extends VideoCallEvent{
        private LocalParticipantEvent() {}
    }

    public static final class LocalVideoTrackUpdated extends LocalParticipantEvent {
        @Nullable
        private final VideoTrack videoTrack;
        private final boolean enable;

        public LocalVideoTrackUpdated(@Nullable VideoTrack videoTrack, boolean enable) {
            this.videoTrack = videoTrack;
            this.enable = enable;
        }

        @Nullable
        public VideoTrack getVideoTrack() {
            return videoTrack;
        }

        public boolean isEnable() {
            return enable;
        }
    }

    public static final class LocalAudioTrackUpdated extends LocalParticipantEvent {
        @Nullable
        private final AudioTrack audioTrack;
        private final boolean enable;

        public LocalAudioTrackUpdated(@Nullable AudioTrack audioTrack, boolean enable) {
            this.audioTrack = audioTrack;
            this.enable = enable;
        }

        @Nullable
        public AudioTrack getAudioTrack() {
            return audioTrack;
        }

        public boolean isEnable() {
            return enable;
        }
    }

}

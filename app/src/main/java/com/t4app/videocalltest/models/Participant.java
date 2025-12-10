package com.t4app.videocalltest.models;

import androidx.annotation.NonNull;

import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.List;

public interface Participant {

    enum State{
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        DISCONNECTED
    }
    @NonNull
    State getState();

    @NonNull
    List<VideoTrack> getVideoTracks();

    @NonNull
    List<AudioTrack> getAudioTracks();

}

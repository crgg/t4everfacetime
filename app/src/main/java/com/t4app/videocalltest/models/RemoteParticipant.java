package com.t4app.videocalltest.models;

import androidx.annotation.NonNull;

import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.Collections;
import java.util.List;

public class RemoteParticipant implements Participant{

    private String name;
    private State state;
    private List<VideoTrack> videoTrackList;
    private List<AudioTrack> audioTrackList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setVideoTrackList(List<VideoTrack> videoTrackList) {
        this.videoTrackList = videoTrackList;
    }

    public void setAudioTrackList(List<AudioTrack> audioTrackList) {
        this.audioTrackList = audioTrackList;
    }

    @NonNull
    @Override
    public State getState() {
        return state;
    }

    @NonNull
    @Override
    public List<VideoTrack> getVideoTracks() {
        return videoTrackList;
    }

    @NonNull
    @Override
    public List<AudioTrack> getAudioTracks() {
        return audioTrackList;
    }
}

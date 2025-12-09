package com.t4app.videocalltest.models;

import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

public class LocalParticipant {
    private String name;

    private VideoTrack videoTrack;
    private AudioTrack audioTrack;
    private boolean videoEnable;
    private boolean audioEnable;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

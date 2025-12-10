package com.t4app.videocalltest.viewmodel;

import com.t4app.videocalltest.models.Participant;
import com.t4app.videocalltest.viewmodel.manager.VideoCallManager;

import java.util.List;

public class VideoCallManagerHolder {

    private static VideoCallManager instance;
    private static List<Participant> participants;

    public static void set(VideoCallManager rm) {
        instance = rm;
    }

    public static VideoCallManager get() {
        return instance;
    }

    public static List<Participant> getParticipants() {
        return participants;
    }

    public static void setParticipants(List<Participant> participants) {
        VideoCallManagerHolder.participants = participants;
    }
}

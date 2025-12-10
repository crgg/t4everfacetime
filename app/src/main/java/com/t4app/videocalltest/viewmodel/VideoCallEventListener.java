package com.t4app.videocalltest.viewmodel;

import com.t4app.videocalltest.events.VideoCallEvent;

public interface VideoCallEventListener {
    void onCallEvent(VideoCallEvent videoCallEvent);
}

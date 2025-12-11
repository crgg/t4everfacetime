package com.t4app.t4videocall.viewmodel;

import com.t4app.t4videocall.events.VideoCallEvent;

public interface VideoCallEventListener {
    void onCallEvent(VideoCallEvent videoCallEvent);
}

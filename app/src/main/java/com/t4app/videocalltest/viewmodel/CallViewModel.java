package com.t4app.videocalltest.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CallViewModel extends ViewModel implements VideoCallEventListener{

    private final MutableLiveData<VideoCallEvent> _event = new MutableLiveData<>();
    public LiveData<VideoCallEvent> events = _event;
    private VideoCallManager callManager;

    public CallViewModel(VideoCallManager callManager) {
        this.callManager = callManager;

        this.callManager.setListener(this);
    }

    public void sendEvent(VideoCallEvent event){
        _event.postValue(event);
    }


    @Override
    public void onCallEvent(VideoCallEvent videoCallEvent) {
        sendEvent(videoCallEvent);
    }


    public void processInput(VideoCallViewEvent event){
        if (event instanceof VideoCallViewEvent.Connected){
            VideoCallViewEvent.Connected videoCall = (VideoCallViewEvent.Connected) event;
            callManager.createRoom(videoCall.getRoomName(), videoCall.getUserName());

        } else if (event instanceof VideoCallViewEvent.Disconnect){

            VideoCallViewEvent.Disconnect videoCall = (VideoCallViewEvent.Disconnect) event;
            callManager.leaveRoom(videoCall.getRoomName(), videoCall.getUserName());

        } else if (event instanceof VideoCallViewEvent.JoinRoom){

            VideoCallViewEvent.JoinRoom videoCall = (VideoCallViewEvent.JoinRoom) event;
            callManager.joinRoom(videoCall.getRoomName(), videoCall.getUserName(), true);

        } else if (event instanceof VideoCallViewEvent.ToggleLocalVideo){

            VideoCallViewEvent.ToggleLocalVideo videoCall = (VideoCallViewEvent.ToggleLocalVideo) event;
            callManager.sendToggleVideoEvent(videoCall);

        } else if (event instanceof VideoCallViewEvent.ToggleLocalAudio){

            VideoCallViewEvent.ToggleLocalAudio videoCall = (VideoCallViewEvent.ToggleLocalAudio) event;
            callManager.sendToggleAudioEvent(videoCall);

        }
    }

}

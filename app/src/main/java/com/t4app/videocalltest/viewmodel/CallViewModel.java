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
        if (videoCallEvent instanceof VideoCallEvent.Connecting){
            callManager.createPeerConnection();
            sendEvent(videoCallEvent);
        } else if (videoCallEvent instanceof VideoCallEvent.Connected){
            sendEvent(videoCallEvent);
        } else if (videoCallEvent instanceof VideoCallEvent.CallerConnected){
            sendEvent(videoCallEvent);
        } else if (videoCallEvent instanceof VideoCallEvent.Disconnect) {
            sendEvent(videoCallEvent);
        } else if (videoCallEvent instanceof VideoCallEvent.RemoteDisconnect){
            sendEvent(videoCallEvent);
        }else if (videoCallEvent instanceof VideoCallEvent.SendIceCandidate){
            sendEvent(videoCallEvent);
        }else if (videoCallEvent instanceof VideoCallEvent.AddLocalUser){
            sendEvent(videoCallEvent);
        }else if (videoCallEvent instanceof VideoCallEvent.AddRemoteUser){
            sendEvent(videoCallEvent);
        }
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
        }
    }

}

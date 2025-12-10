package com.t4app.videocalltest.viewmodel;

import android.Manifest;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.t4app.videocalltest.PermissionUtil;
import com.t4app.videocalltest.VideoService;
import com.t4app.videocalltest.events.VideoCallEvent;
import com.t4app.videocalltest.events.VideoCallViewEvent;
import com.t4app.videocalltest.models.Room;
import com.t4app.videocalltest.viewmodel.manager.VideoCallManager;

public class CallViewModel extends ViewModel implements VideoCallEventListener{
    private static final String TAG = "CALL_VIEW_MODEL";

    private final SingleLiveEvent<VideoCallEvent> _event = new SingleLiveEvent<>();
    public LiveData<VideoCallEvent> events = _event;
    private VideoCallManager callManager;
    private PermissionUtil permissionUtil;

    private Room room;

    public CallViewModel(VideoCallManager callManager, PermissionUtil permissionUtil) {
        this.callManager = callManager;
        this.permissionUtil = permissionUtil;
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
        if (event instanceof VideoCallViewEvent.OnResume){

            checkStatusCall();

        }else if (event instanceof VideoCallViewEvent.OnPause){

            callManager.onPause();

        }else if (event instanceof VideoCallViewEvent.CreateRoom){
            VideoCallViewEvent.CreateRoom videoCall = (VideoCallViewEvent.CreateRoom) event;
            callManager.createRoom(videoCall.getRoomName(), videoCall.getUserName());

        } else if (event instanceof VideoCallViewEvent.Connected){
            VideoCallViewEvent.Connected videoCall = (VideoCallViewEvent.Connected) event;
//            callManager.createRoom(videoCall.getRoomName(), videoCall.getUserName());

        } else if (event instanceof VideoCallViewEvent.Disconnect){

            VideoCallViewEvent.Disconnect videoCall = (VideoCallViewEvent.Disconnect) event;
            callManager.leaveRoom(videoCall.getRoomName(), videoCall.getUserName());

        } else if (event instanceof VideoCallViewEvent.JoinRoom){

            VideoCallViewEvent.JoinRoom videoCall = (VideoCallViewEvent.JoinRoom) event;
            callManager.joinRoom(videoCall.getRoomName(), videoCall.getUserName(), true);

        } else if (event instanceof VideoCallViewEvent.ToggleLocalVideo){

            callManager.sendToggleVideoEvent();

        } else if (event instanceof VideoCallViewEvent.ToggleLocalAudio){

            callManager.sendToggleAudioEvent();

        }
    }


    private void checkStatusCall(){
        boolean isCameraEnable = permissionUtil.isPermissionGranted(Manifest.permission.CAMERA);
        boolean isMicEnable = permissionUtil.isPermissionGranted(Manifest.permission.RECORD_AUDIO);

        if (VideoService.isServiceStarted){
            if (VideoCallManagerHolder.get() != null && VideoCallManagerHolder.get().room != null){
                room = VideoCallManagerHolder.get().room;
                Room.State state = room.getState();
                switch (state){
                    case CONNECTING:
                        break;
                    case CONNECTED:
                        VideoCallManagerHolder.get().sendVideoCallEvent(
                                new VideoCallEvent.RoomConnected(room.getName()));
                        break;
                    case DISCONNECTED:
                        VideoCallManagerHolder.get().sendVideoCallEvent(new VideoCallEvent.Disconnect());
                        break;

                }
            }
        }

        if (isCameraEnable && isMicEnable){
            Log.d(TAG, "IS ENABLE ");
            callManager.onResume();
        }

    }

}

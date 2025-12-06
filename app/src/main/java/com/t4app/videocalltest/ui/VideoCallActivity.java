package com.t4app.videocalltest.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.t4app.videocalltest.SfuWebSocketClient;
import com.t4app.videocalltest.databinding.ActivityVideoCallBinding;
import com.t4app.videocalltest.retrofit.ApiServices;
import com.t4app.videocalltest.retrofit.RetrofitClient;
import com.t4app.videocalltest.viewmodel.CallViewModel;
import com.t4app.videocalltest.viewmodel.CallViewModelFactory;
import com.t4app.videocalltest.viewmodel.VideoCallEvent;
import com.t4app.videocalltest.viewmodel.VideoCallManager;
import com.t4app.videocalltest.viewmodel.VideoCallViewEvent;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.List;

public class VideoCallActivity extends AppCompatActivity {
    private final static String TAG = "VIDEO_CALL_ACT";

    private ActivityVideoCallBinding binding;

    private String globalRoom = "";
    private String globalUserName = "";
    private boolean inRoom = false;
    private ApiServices apiServices = RetrofitClient.getRetrofitClient().create(ApiServices.class);

    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private EglBase eglBase;

    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;

    private VideoTrack remoteVideoTrack;

    private boolean iAmCaller = false;
    private boolean hasJoinedRoom = false;

    private SurfaceViewRenderer mainRender;
    private SurfaceViewRenderer pinRender;

    private CallViewModel viewModel;

    private boolean isLocalInMain;

    private VideoCallManager videoCallManager;

    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainRender = binding.mainRender;
        pinRender = binding.pinRender;

        initWebRTC();

        videoCallManager = new VideoCallManager(globalUserName,
                globalRoom, peerConnection, peerConnectionFactory);

        CallViewModelFactory factory = new CallViewModelFactory(videoCallManager);

        viewModel = new ViewModelProvider(this, factory)
                .get(CallViewModel.class);

        observeEvents();

        binding.createRoomBtn.setOnClickListener(view -> {
            String roomName = binding.roomNameEt.getText().toString();
            String userName = binding.yourNameEt.getText().toString();

            if (!roomName.isEmpty() && !userName.isEmpty()){
                viewModel.processInput(new VideoCallViewEvent.Connected(roomName, userName));
            }
        });

        binding.joinRoomBtn.setOnClickListener(view -> {
            String roomName = binding.roomNameEt.getText().toString();
            String userName = binding.yourNameEt.getText().toString();

            if (!roomName.isEmpty() && !userName.isEmpty()){
                viewModel.processInput(new VideoCallViewEvent.JoinRoom(roomName, userName));
            }
        });

        binding.leaveRoomBtn.setOnClickListener(view -> {
            String roomName = binding.roomNameEt.getText().toString();
            String userName = binding.yourNameEt.getText().toString();

            if (!roomName.isEmpty() && !userName.isEmpty()){
                viewModel.processInput(new VideoCallViewEvent.Disconnect(roomName, userName));
            }
        });

        mainRender.setOnClickListener(view -> swapVideo());

        pinRender.setOnClickListener(view -> swapVideo());

    }

    private void initWebRTC() {
        eglBase = EglBase.create();

        mainRender.init(eglBase.getEglBaseContext(), null);
        mainRender.setZOrderMediaOverlay(false);
        isLocalInMain = true;

        pinRender.init(eglBase.getEglBaseContext(), null);
        pinRender.setZOrderMediaOverlay(true);

        PeerConnectionFactory.InitializationOptions initOptions =
                PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
                        .setEnableInternalTracer(true)
                        .createInitializationOptions();

        PeerConnectionFactory.initialize(initOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory encoderFactory =
                new DefaultVideoEncoderFactory(
                        eglBase.getEglBaseContext(),
                        /* enableIntelVp8Encoder */ true,
                        /* enableH264HighProfile */ true
                );

        DefaultVideoDecoderFactory decoderFactory =
                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        createLocalMediaTracks();
    }

    private void observeEvents(){
        viewModel.events.observe(this, videoCallEvent -> {
            if (videoCallEvent instanceof VideoCallEvent.Connected){
                VideoCallEvent.Connected videoCall = (VideoCallEvent.Connected) videoCallEvent;

                StringBuilder textInfo = new StringBuilder("INFO ROOM\nRoom Created RoomName: " +
                        videoCall.getRoomInfo().getName());
                for (String participant : videoCall.getRoomInfo().getParticipants()) {
                    String formatedString = "\nParticipant: " + participant;
                    textInfo.append(formatedString);
                }

                iAmCaller = videoCall.isiAmCaller();
                inRoom = videoCall.isInRoom();

                connected = true;
                hasJoinedRoom = true;
                globalRoom = videoCall.getRoomInfo().getName();
                globalUserName = videoCall.getName();
                inRoom = true;
                binding.infoRoom.setText(textInfo);

                binding.leaveRoomBtn.setVisibility(View.VISIBLE);
                binding.createRoomBtn.setVisibility(View.GONE);
                binding.joinRoomBtn.setVisibility(View.GONE);

            } else if (videoCallEvent instanceof VideoCallEvent.CallerConnected){
                if (!connected){
                    videoCallManager.createPeerConnection();
                }
                videoCallManager.createAndSendOffer();
            } else if (videoCallEvent instanceof VideoCallEvent.Disconnect) {
                clearViews();
            } else if (videoCallEvent instanceof VideoCallEvent.RemoteDisconnect){
                clearViews();
            }else if (videoCallEvent instanceof VideoCallEvent.SendIceCandidate){
                VideoCallEvent.SendIceCandidate sendIceCandidate = (VideoCallEvent.SendIceCandidate) videoCallEvent;
                videoCallManager.sendIceCandidate(sendIceCandidate.getIceCandidate());

            }else if (videoCallEvent instanceof VideoCallEvent.AddLocalUser){
                VideoCallEvent.AddLocalUser addLocalUser = (VideoCallEvent.AddLocalUser) videoCallEvent;
                List<String> streamIds = addLocalUser.getStreamIds();
                if (peerConnection != null){
                    peerConnection.addTrack(localVideoTrack, streamIds);
                    peerConnection.addTrack(localAudioTrack, streamIds);
                }
            }else if (videoCallEvent instanceof VideoCallEvent.AddRemoteUser){
                Log.d(TAG, "ADD REMOTE: ");
                VideoCallEvent.AddRemoteUser remoteUser = (VideoCallEvent.AddRemoteUser) videoCallEvent;
                remoteVideoTrack = remoteUser.getRemoteVideoTrack();
                isLocalInMain = remoteUser.isLocalInRoom();
                attachRemoteVideo();
            }
        });
    }


    private void swapVideo(){
        if (localVideoTrack == null || remoteVideoTrack == null) return;

        if (isLocalInMain){
            localVideoTrack.removeSink(mainRender);
            remoteVideoTrack.removeSink(pinRender);

            localVideoTrack.addSink(pinRender);
            remoteVideoTrack.addSink(mainRender);

            mainRender.setMirror(false);
            pinRender.setMirror(true);

            isLocalInMain = false;
        }else{
            remoteVideoTrack.removeSink(mainRender);
            localVideoTrack.removeSink(pinRender);

            remoteVideoTrack.addSink(pinRender);
            localVideoTrack.addSink(mainRender);

            mainRender.setMirror(true);
            pinRender.setMirror(false);

            isLocalInMain = true;
        }

        mainRender.requestLayout();
        pinRender.requestLayout();
    }

    private void createLocalMediaTracks(){
        VideoCapturer videoCapturer = createCameraCapturer();
        if (videoCapturer == null){
            Log.d(TAG, "NULL VIDEO CAPTURER");
            return;
        }

        SurfaceTextureHelper surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());

        videoSource = peerConnectionFactory.createVideoSource(false);
        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());

        try {
            videoCapturer.startCapture(640, 480, 30);
        }catch (Exception e){
            Log.e(TAG, "ERROR START CAPTURE: ", e);
        }

        localVideoTrack = peerConnectionFactory.createVideoTrack("video_track", videoSource);
        localVideoTrack.addSink(mainRender);
        mainRender.setMirror(true);
        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio_track", audioSource);
    }

    private VideoCapturer createCameraCapturer(){
        CameraEnumerator enumerator;
        if (Camera2Enumerator.isSupported(this)){
            enumerator = new Camera2Enumerator(this);
        }else{
            enumerator = new Camera1Enumerator(true);
        }

        for (String deviceName : enumerator.getDeviceNames()){
            if (enumerator.isFrontFacing(deviceName)){
                VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
                if (capturer != null) return  capturer;
            }
        }

        for (String deviceName : enumerator.getDeviceNames()){
            VideoCapturer capturer = enumerator.createCapturer(deviceName, null);
            if (capturer != null) return  capturer;
        }

        return null;
    }

    private void attachRemoteVideo() {
        if (remoteVideoTrack == null) return;

        remoteVideoTrack.removeSink(mainRender);
        remoteVideoTrack.removeSink(pinRender);

        if (isLocalInMain) {
            remoteVideoTrack.addSink(pinRender);
            mainRender.setMirror(true);
            pinRender.setMirror(false);
        } else {
            remoteVideoTrack.addSink(mainRender);
            mainRender.setMirror(false);
            pinRender.setMirror(true);
        }

        pinRender.setVisibility(View.VISIBLE);
    }


    private void clearViews(){
        binding.infoRoom.setText("Leave the room");
        globalRoom = "";
        globalUserName = "";

        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeSink(mainRender);
            remoteVideoTrack.removeSink(pinRender);
        }

        pinRender.release();
        pinRender.setVisibility(View.GONE);

        isLocalInMain = true;
        swapVideo();
        binding.leaveRoomBtn.setVisibility(View.GONE);
        binding.createRoomBtn.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onStop() {
        super.onStop();
//        if (inRoom) {
//            leaveRoom(globalRoom, globalUserName);
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (peerConnection != null) {
            peerConnection.close();
        }

        if (localVideoTrack != null) {
            localVideoTrack.removeSink(mainRender);
            localVideoTrack.removeSink(pinRender);
        }
        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeSink(mainRender);
            remoteVideoTrack.removeSink(pinRender);
        }

        mainRender.release();
        pinRender.release();

        if (eglBase != null) {
            eglBase.release();
        }

    }
}
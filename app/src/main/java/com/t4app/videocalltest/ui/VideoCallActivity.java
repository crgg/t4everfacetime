package com.t4app.videocalltest.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.t4app.videocalltest.R;
import com.t4app.videocalltest.databinding.ActivityVideoCallBinding;
import com.t4app.videocalltest.viewmodel.CallViewModel;
import com.t4app.videocalltest.viewmodel.CallViewModelFactory;
import com.t4app.videocalltest.viewmodel.VideoCallEvent;
import com.t4app.videocalltest.viewmodel.VideoCallManager;
import com.t4app.videocalltest.viewmodel.VideoCallViewEvent;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.List;

public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VIDEO_CALL_ACT";

    private ActivityVideoCallBinding binding;
    private SurfaceViewRenderer mainRender;
    private SurfaceViewRenderer pinRender;

    private String globalRoom = "";
    private String globalUserName = "";
    private boolean inRoom = false;
    private boolean isIncoming = false;
    private boolean iAmCaller = false;
    private boolean hasJoinedRoom = false;
    private boolean isLocalInMain = false;
    private boolean connected = false;
    private boolean videoEnable = false;
    private boolean micEnable = false;
    private boolean speakerEnable = true;

    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private EglBase eglBase;

    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;

    private VideoTrack remoteVideoTrack;


    private VideoCallManager videoCallManager;
    private CallViewModel viewModel;


    private ActivityResultLauncher<String[]> permissionsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainRender = binding.mainRender;
        pinRender = binding.pinRender;

        permissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {

                    boolean audio = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));
                    boolean camera = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));

                    if (audio && camera) {
                        if (isIncoming){
                            viewModel.processInput(new VideoCallViewEvent.IncomingCall(globalRoom, globalUserName));
                        }else{
                            viewModel.processInput(new VideoCallViewEvent.OnResume());
                            createLocalMediaTracks();
                        }
                    }
                }
        );

        if (!isIncoming){
            if (!checkPermissionForCameraAndMicrophone()) {
                requestPermissions();
                micEnable = false;
                videoEnable = false;
            }else{
                micEnable = true;
                videoEnable = true;
            }
        }

        initWebRTC();
        bindVideoBtn();
        bindAudioBtn();
        bindMicBtn();


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

            //TODO:FINISH ACTIVITY HERE NOT NOW BECAUSE TEST
            if (!roomName.isEmpty() && !userName.isEmpty()){
                viewModel.processInput(new VideoCallViewEvent.Disconnect(roomName, userName));
            }
        });

        binding.leaveRoomBtn.setEnabled(false);

        binding.videoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoEnable = !videoEnable;
                if (localVideoTrack != null){
                    viewModel.processInput(new VideoCallViewEvent.ToggleLocalVideo(globalUserName, videoEnable));
                }
//                bindVideoBtn(); TODO:CHANGE THIS IN PROCESS EVENT LOGIC

            }
        });

        binding.micBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                micEnable = !micEnable;
                if (localAudioTrack != null){
                    viewModel.processInput(new VideoCallViewEvent.ToggleLocalAudio(globalUserName, micEnable));
                }
//                bindMicBtn(); TODO:CHANGE THIS IN PROCESS EVENT LOGIC
            }
        });

        binding.soundBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speakerEnable = !speakerEnable;
                bindAudioBtn();
            }
        });

        mainRender.setOnClickListener(view -> swapVideo());

        pinRender.setOnClickListener(view -> swapVideo());

        if (isIncoming){
            globalRoom = getIntent().getStringExtra("room_name");
            if (checkPermissionForCameraAndMicrophone()){
                viewModel.processInput(new VideoCallViewEvent.IncomingCall(globalRoom, globalUserName));
            }else{
                requestPermissions();
            }
        }

    }

    private void bindMicBtn(){
        localAudioTrack.setEnabled(micEnable);
        if (micEnable){
            binding.micBtn.setImageResource(R.drawable.ic_mic);
            int micIconColor = ContextCompat.getColor(this, R.color.white);
            int bgMicColor   = ContextCompat.getColor(this, R.color.action_button);

            binding.micBtn.setImageTintList(ColorStateList.valueOf(micIconColor));
            binding.micBtn.setBackgroundTintList(ColorStateList.valueOf(bgMicColor));
        }else{
            int micIconColor = ContextCompat.getColor(this, R.color.red);
            int bgMicColor   = ContextCompat.getColor(this, R.color.white);

            binding.micBtn.setImageResource(R.drawable.ic_mic_off);
            binding.micBtn.setImageTintList(ColorStateList.valueOf(micIconColor));
            binding.micBtn.setBackgroundTintList(ColorStateList.valueOf(bgMicColor));
        }

    }


    private void bindVideoBtn(){
        localVideoTrack.setEnabled(videoEnable);
        if (videoEnable){
            binding.videoBtn.setImageResource(R.drawable.ic_video_cam);

            int videoIconColor = ContextCompat.getColor(this, R.color.black);
            int bgVideoColor   = ContextCompat.getColor(this, R.color.white);

            binding.videoBtn.setImageTintList(ColorStateList.valueOf(videoIconColor));
            binding.videoBtn.setBackgroundTintList(ColorStateList.valueOf(bgVideoColor));
        }else{
            binding.videoBtn.setImageResource(R.drawable.ic_video_cam_off);

            int videoIconColor = ContextCompat.getColor(this, R.color.white);
            int bgVideoColor   = ContextCompat.getColor(this, R.color.action_button);

            binding.videoBtn.setImageTintList(ColorStateList.valueOf(videoIconColor));
            binding.videoBtn.setBackgroundTintList(ColorStateList.valueOf(bgVideoColor));
        }

    }

    private void bindAudioBtn(){
        if (speakerEnable){
            binding.soundBtn.setImageResource(R.drawable.ic_max_sound);

            int videoIconColor = ContextCompat.getColor(this, R.color.black);
            int bgVideoColor   = ContextCompat.getColor(this, R.color.white);

            binding.soundBtn.setImageTintList(ColorStateList.valueOf(videoIconColor));
            binding.soundBtn.setBackgroundTintList(ColorStateList.valueOf(bgVideoColor));
        }else{
            binding.soundBtn.setImageResource(R.drawable.ic_mute);

            int videoIconColor = ContextCompat.getColor(this, R.color.white);
            int bgVideoColor   = ContextCompat.getColor(this, R.color.action_button);

            binding.soundBtn.setImageTintList(ColorStateList.valueOf(videoIconColor));
            binding.soundBtn.setBackgroundTintList(ColorStateList.valueOf(bgVideoColor));
        }

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

                binding.leaveRoomBtn.setEnabled(true);

            } else if (videoCallEvent instanceof VideoCallEvent.CallerConnected){

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
            }else if (videoCallEvent instanceof VideoCallEvent.ToggleLocalAudio){
                Log.d(TAG, "TOGGLE LOCAL AUDIO: ");
                bindMicBtn();
            }else if (videoCallEvent instanceof VideoCallEvent.ToggleLocalVideo){
                Log.d(TAG, "TOGGLE LOCAL VIDEO:");
                bindVideoBtn();

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
        if (remoteVideoTrack == null) {
            Log.w(TAG, "attachRemoteVideo: remoteVideoTrack es null");
            return;
        }

        Log.d(TAG, "attachRemoteVideo: local=" + (localVideoTrack != null));

        remoteVideoTrack.removeSink(mainRender);
        remoteVideoTrack.removeSink(pinRender);

        if (localVideoTrack != null) {
            localVideoTrack.removeSink(mainRender);
            localVideoTrack.removeSink(pinRender);
            localVideoTrack.addSink(pinRender);
        }

        remoteVideoTrack.addSink(mainRender);

        mainRender.setMirror(false);
        pinRender.setMirror(true);

        isLocalInMain = false;

        pinRender.setVisibility(View.VISIBLE);
        mainRender.setVisibility(View.VISIBLE);
    }

    private void clearViews(){
        binding.infoRoom.setText("Leave the room");
        globalRoom = "";
        globalUserName = "";

        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeSink(mainRender);
            remoteVideoTrack.removeSink(pinRender);
        }

        pinRender.setVisibility(View.GONE);

        isLocalInMain = true;
        attachMainView();

        binding.leaveRoomBtn.setVisibility(View.GONE);
        binding.createRoomBtn.setVisibility(View.VISIBLE);
    }

    private void attachMainView(){
        if (localVideoTrack == null) return;

        localVideoTrack.removeSink(mainRender);
        localVideoTrack.removeSink(pinRender);

        localVideoTrack.addSink(mainRender);
        mainRender.setMirror(true);
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }


    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permissionsLauncher.launch(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.POST_NOTIFICATIONS,
            });

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissionsLauncher.launch(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.BLUETOOTH_CONNECT,
            });

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            permissionsLauncher.launch(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
            });
        }
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
package com.t4app.videocalltest.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.t4app.videocalltest.MessagesUtils;
import com.t4app.videocalltest.R;
import com.t4app.videocalltest.databinding.ActivityVideoCallBinding;
import com.t4app.videocalltest.models.LocalParticipant;
import com.t4app.videocalltest.models.RemoteParticipant;
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

import java.util.ArrayList;
import java.util.List;

public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VIDEO_CALL_ACT";

    private ActivityVideoCallBinding binding;
    private MainParticipantView mainRender;
    private ParticipantView pinRender;

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

    private LocalParticipant localParticipant;
    private RemoteParticipant remoteParticipant;

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

        mainRender = binding.mainContainer;
        pinRender = binding.pinContainer;

        remoteParticipant = new RemoteParticipant();
        localParticipant = new LocalParticipant();

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
        bindMicBtn();


        videoCallManager = new VideoCallManager(globalUserName,
                globalRoom, peerConnection, peerConnectionFactory, localVideoTrack, localAudioTrack);

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

        binding.videoBtn.setOnClickListener(view -> {
            videoEnable = !videoEnable;
            if (localVideoTrack != null){
                viewModel.processInput(new VideoCallViewEvent.ToggleLocalVideo(globalUserName, videoEnable));
            }
        });

        binding.micBtn.setOnClickListener(view -> {
            micEnable = !micEnable;
            if (localAudioTrack != null){
                viewModel.processInput(new VideoCallViewEvent.ToggleLocalAudio(globalUserName, micEnable));
            }
        });

        binding.soundBtn.setOnClickListener(view -> {
            speakerEnable = !speakerEnable;
            getAvailableAudioDevices();

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

    private void getAvailableAudioDevices(){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        List<String> names = new ArrayList<>();

        List<AudioDeviceInfo> deviceList = new ArrayList<>();

        for (AudioDeviceInfo device : devices){
            String label = "";

            switch (device.getType()){
                case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                    label = "Earpiece";
                    break;
                case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                    label = "Speaker";
                    break;
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                    label = "Wired headphones";
                    break;
                case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                    label = "Bluetooth (calls)";
                    break;
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                    label = "Bluetooth (audio)";
                    break;
                default:
                    label = "Other: " + device.getProductName();
            }

            names.add(label);
            deviceList.add(device);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select audio output");

        builder.setItems(names.toArray(new String[0]), (dialog, which) ->{
            AudioDeviceInfo selected = deviceList.get(which);
            setAudioOutput(selected);
//            viewModel.processInput(new VideoCallViewEvent.ChangeAudioOutput(selected));

        });

        builder.show();
    }

    private void setAudioOutput(AudioDeviceInfo device) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        switch (device.getType()) {
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                binding.soundBtn.setImageResource(R.drawable.ic_headphone);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setSpeakerphoneOn(false);
                break;

            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                binding.soundBtn.setImageResource(R.drawable.ic_max_sound);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.setSpeakerphoneOn(true);
                break;

            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                binding.soundBtn.setImageResource(R.drawable.ic_headphone);
                audioManager.setSpeakerphoneOn(false);
                break;

            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                binding.soundBtn.setImageResource(R.drawable.ic_bluetooth_speaker);
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.startBluetoothSco();
                audioManager.setBluetoothScoOn(true);
                break;

            default:
                MessagesUtils.showErrorDialog(VideoCallActivity.this, "Device not supported");
        }

    }


    private void bindMicBtn(){
        localAudioTrack.setEnabled(micEnable);
        if (isLocalInMain){
            mainRender.setMuted(micEnable);
            if (micEnable){
                mainRender.setMutedText("You are muted");
            }
        }else{
            pinRender.setMuted(micEnable);
        }
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
        if (isLocalInMain){
            if (videoEnable){
                mainRender.setState(ParticipantView.State.VIDEO);
            }else{
                mainRender.setState(ParticipantView.State.NO_VIDEO);
            }
        }else{
            if (videoEnable){
                pinRender.setState(ParticipantView.State.VIDEO);
            }else{
                pinRender.setState(ParticipantView.State.NO_VIDEO);
            }
        }

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

    private void initWebRTC() {
        eglBase = EglBase.create();

        mainRender.getVideoView().init(eglBase.getEglBaseContext(), null);
        mainRender.getVideoView().setZOrderMediaOverlay(false);
        isLocalInMain = true;

        pinRender.getVideoView().init(eglBase.getEglBaseContext(), null);
        pinRender.getVideoView().setZOrderMediaOverlay(true);

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

//                VideoCallEvent.AddLocalUser addLocalUser = (VideoCallEvent.AddLocalUser) videoCallEvent;
//                List<String> streamIds = addLocalUser.getStreamIds();
//                if (peerConnection != null){
//                    peerConnection.addTrack(localVideoTrack, streamIds);
//                    peerConnection.addTrack(localAudioTrack, streamIds);
//                }

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

            }else if (videoCallEvent instanceof VideoCallEvent.ToggleRemoteAudio){

                VideoCallEvent.ToggleRemoteAudio audio = (VideoCallEvent.ToggleRemoteAudio) videoCallEvent;
                toggleRemoteAudio(audio.isAudioStatus());


            }else if (videoCallEvent instanceof VideoCallEvent.ToggleRemoteVideo){

                VideoCallEvent.ToggleRemoteVideo video = (VideoCallEvent.ToggleRemoteVideo) videoCallEvent;
                toggleRemoteVideo(video.isVideoStatus());

            }else if (videoCallEvent instanceof VideoCallEvent.UserJoined){


            }else if (videoCallEvent instanceof VideoCallEvent.UserLeave){


            }
        });
    }

    private void toggleRemoteVideo(boolean status) {
        if (isLocalInMain){
            if (status){
                pinRender.setState(ParticipantView.State.VIDEO);
            }else{
                pinRender.setState(ParticipantView.State.NO_VIDEO);
            }
        }else{
            if (status){
                mainRender.setState(ParticipantView.State.VIDEO);
            }else{
                mainRender.setState(ParticipantView.State.NO_VIDEO);
            }
        }
    }



    private void toggleRemoteAudio(boolean status) {
        if (isLocalInMain){
            pinRender.setMuted(status);
        }else{
            mainRender.setMuted(status);
            mainRender.setMutedText(remoteParticipant.getName() + " is muted");
        }
    }

    private ParticipantView getRemoteView() {
        return isLocalInMain ? binding.pinContainer : binding.mainContainer;
    }



    private void swapVideo(){
        if (localVideoTrack == null || remoteVideoTrack == null) return;

        if (isLocalInMain){
            localVideoTrack.removeSink(mainRender.getVideoView());
            remoteVideoTrack.removeSink(pinRender.getVideoView());

            localVideoTrack.addSink(pinRender.getVideoView());
            remoteVideoTrack.addSink(mainRender.getVideoView());

            mainRender.setMirror(false);
            pinRender.setMirror(true);

            isLocalInMain = false;
        }else{
            remoteVideoTrack.removeSink(mainRender.getVideoView());
            localVideoTrack.removeSink(pinRender.getVideoView());

            remoteVideoTrack.addSink(pinRender.getVideoView());
            localVideoTrack.addSink(mainRender.getVideoView());

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
        localVideoTrack.addSink(mainRender.getVideoView());
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

        remoteVideoTrack.removeSink(mainRender.getVideoView());
        remoteVideoTrack.removeSink(pinRender.getVideoView());

        if (localVideoTrack != null) {
            localVideoTrack.removeSink(mainRender.getVideoView());
            localVideoTrack.removeSink(pinRender.getVideoView());
            localVideoTrack.addSink(pinRender.getVideoView());
        }

        remoteVideoTrack.addSink(mainRender.getVideoView());

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
            remoteVideoTrack.removeSink(mainRender.getVideoView());
            remoteVideoTrack.removeSink(pinRender.getVideoView());
        }

        pinRender.setVisibility(View.GONE);

        isLocalInMain = true;
        attachMainView();

        binding.leaveRoomBtn.setVisibility(View.GONE);
        binding.createRoomBtn.setVisibility(View.VISIBLE);
    }

    private void attachMainView(){
        if (localVideoTrack == null) return;

        localVideoTrack.removeSink(mainRender.getVideoView());
        localVideoTrack.removeSink(pinRender.getVideoView());

        localVideoTrack.addSink(mainRender.getVideoView());
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
            localVideoTrack.removeSink(mainRender.getVideoView());
            localVideoTrack.removeSink(pinRender.getVideoView());
        }
        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeSink(mainRender.getVideoView());
            remoteVideoTrack.removeSink(pinRender.getVideoView());
        }

        mainRender.getVideoView().release();
        pinRender.getVideoView().release();

        if (eglBase != null) {
            eglBase.release();
        }

    }
}
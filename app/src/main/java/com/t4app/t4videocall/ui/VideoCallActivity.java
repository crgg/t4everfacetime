package com.t4app.t4videocall.ui;

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

import com.t4app.t4videocall.MessagesUtils;
import com.t4app.t4videocall.PermissionUtil;
import com.t4app.t4videocall.R;
import com.t4app.t4videocall.SafeClickListener;
import com.t4app.t4videocall.VideoService;
import com.t4app.t4videocall.databinding.ActivityVideoCallBinding;
import com.t4app.t4videocall.events.CallingRingtoneManager;
import com.t4app.t4videocall.events.VideoCallEvent;
import com.t4app.t4videocall.events.VideoCallViewEvent;
import com.t4app.t4videocall.models.LocalParticipant;
import com.t4app.t4videocall.models.Participant;
import com.t4app.t4videocall.models.RemoteParticipant;
import com.t4app.t4videocall.models.Room;
import com.t4app.t4videocall.viewmodel.CallViewModel;
import com.t4app.t4videocall.viewmodel.CallViewModelFactory;
import com.t4app.t4videocall.viewmodel.VideoCallManagerHolder;
import com.t4app.t4videocall.viewmodel.manager.VideoCallManager;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class VideoCallActivity extends AppCompatActivity {

    private static final String TAG = "VIDEO_CALL_ACT";

    private ActivityVideoCallBinding binding;
    private MainParticipantView mainRender;
    private ParticipantView pinRender;

    private boolean isIncoming = false;

    private PeerConnectionFactory peerConnectionFactory;
    private EglBase eglBase;

    private VideoCallManager videoCallManager;
    private CallViewModel viewModel;
    private VideoTrack localVideoTrack;

    private VideoTrack remoteVideoTrack;
    private RemoteParticipant remoteParticipant;
    private LocalParticipant localParticipant;

    private List<Participant> participants;

    private ActivityResultLauncher<String[]> permissionsLauncher;
    private Room sessionState;

    private CallingRingtoneManager callingRingtoneManager;

    private String callingTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionState = new Room();

        boolean iAmCaller = getIntent().getBooleanExtra("iAmCaller", false);
        String roomName = getIntent().getStringExtra("roomName");
        callingTo = getIntent().getStringExtra("callingTo");

        sessionState.setRoomName(roomName);


        mainRender = binding.mainContainer;
        pinRender = binding.pinContainer;

        remoteParticipant = new RemoteParticipant();
        remoteParticipant.setVideoTrackList(new ArrayList<>());
        remoteParticipant.setAudioTrackList(new ArrayList<>());

        permissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {

                    boolean audio = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));
                    boolean camera = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));

                    if (audio && camera) {
                        if (isIncoming){
                            viewModel.processInput(new VideoCallViewEvent.JoinRoom(sessionState));
                        }else{
                            viewModel.processInput(new VideoCallViewEvent.OnResume());
                        }
                    }
                }
        );

        if (!isIncoming){
            if (!checkPermissionForCameraAndMicrophone()) {
                requestPermissions();
            }
        }

        initWebRTC();

        videoCallManager = new VideoCallManager(getApplicationContext());
        videoCallManager.initLocalMedia(peerConnectionFactory, eglBase);

        PermissionUtil permissionUtil = new PermissionUtil(this);

        CallViewModelFactory factory = new CallViewModelFactory(videoCallManager, permissionUtil);

        viewModel = new ViewModelProvider(this, factory)
                .get(CallViewModel.class);

        observeEvents();

        callingRingtoneManager = new CallingRingtoneManager(this);

        if (iAmCaller){
            callingRingtoneManager.startOutgoingCallProcess();
            String userName = getIntent().getStringExtra("userName");
            sessionState.setUserName(userName);
            viewModel.processInput(new VideoCallViewEvent.CreateRoom(sessionState));
        } else if (isIncoming){

            sessionState.setUserName("JoinUsername");

            if (checkPermissionForCameraAndMicrophone()){
                viewModel.processInput(new VideoCallViewEvent.JoinRoom(sessionState));
            }else{
                requestPermissions();
            }
        }

        binding.leaveRoomBtn.setOnClickListener(view -> {
            viewModel.processInput(new VideoCallViewEvent.Disconnect(sessionState));
        });

        binding.videoBtn.setOnClickListener(view -> viewModel.processInput(new VideoCallViewEvent.ToggleLocalVideo()));

        binding.micBtn.setOnClickListener(view -> viewModel.processInput(new VideoCallViewEvent.ToggleLocalAudio()));

        binding.soundBtn.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View v) {
                getAvailableAudioDevices();
            }
        });

        pinRender.setOnClickListener(new SafeClickListener() {
            @Override
            public void onSafeClick(View v) {
                swapVideo();
            }
        });

    }

    private void swapVideo(){
        if (localVideoTrack  != null && remoteVideoTrack != null){
            if (localParticipant.isPinned()){
                localVideoTrack.removeSink(pinRender.videoView);
                remoteVideoTrack.removeSink(mainRender.videoView);

                localVideoTrack.addSink(mainRender.videoView);
                remoteVideoTrack.addSink(pinRender.videoView);

                localParticipant.setPinned(false);
            }else{
                localVideoTrack.removeSink(mainRender.videoView);
                remoteVideoTrack.removeSink(pinRender.videoView);

                localVideoTrack.addSink(pinRender.videoView);
                remoteVideoTrack.addSink(mainRender.videoView);

                localParticipant.setPinned(true);
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

    private void bindMicBtn(boolean status){
        if (!localParticipant.isPinned()){
            mainRender.setMuted(status);
            if (status){
                mainRender.setMutedText("You are muted");
            }
        }else{
            pinRender.setMuted(status);
        }
        if (status){
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

    private void bindVideoBtn(boolean status){
        if (localParticipant.isPinned()){
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
        if (status){
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
        localParticipant = new LocalParticipant();
        localParticipant.setPinned(false);

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

    }

    private void observeEvents(){
        viewModel.events.observe(this, videoCallEvent -> {
            if (videoCallEvent instanceof VideoCallEvent.Connected){
                VideoCallEvent.Connected videoCall = (VideoCallEvent.Connected) videoCallEvent;
                binding.leaveRoomBtn.setEnabled(true);

            } else if (videoCallEvent instanceof VideoCallEvent.CallerConnected){

                videoCallManager.createAndSendOffer();

            } else if (videoCallEvent instanceof VideoCallEvent.Disconnect) {

                if (VideoService.isRunning()){
                    VideoService.stopService(this);
                }
                unattachRemoteUser();
                clearViews();

                finish();

            } else if (videoCallEvent instanceof VideoCallEvent.Connecting) {

                binding.callingToInfo.setText(callingTo);
                binding.statusCallTv.setText("Connecting...");

            }else if (videoCallEvent instanceof VideoCallEvent.SendIceCandidate){

                VideoCallEvent.SendIceCandidate sendIceCandidate = (VideoCallEvent.SendIceCandidate) videoCallEvent;
                videoCallManager.sendIceCandidate(sendIceCandidate.getIceCandidate());

            }else if (videoCallEvent instanceof VideoCallEvent.LocalParticipantEvent){

                handleLocalParticipantEvent((VideoCallEvent.LocalParticipantEvent) videoCallEvent);

            }else if (videoCallEvent instanceof VideoCallEvent.RemoteParticipantEvent){

                handleRemoteParticipantEvent((VideoCallEvent.RemoteParticipantEvent) videoCallEvent);

            }
        });
    }

    private void handleRemoteParticipantEvent(VideoCallEvent.RemoteParticipantEvent event){
        Log.d(TAG, "handleRemoteParticipantEvent: " + event.getClass());
        if (event instanceof VideoCallEvent.RemoteParticipantEvent.AddRemoteUser){
            VideoCallEvent.RemoteParticipantEvent.AddRemoteUser remoteUser =
                    (VideoCallEvent.RemoteParticipantEvent.AddRemoteUser) event;

            remoteVideoTrack = remoteUser.getRemoteVideoTrack();
            remoteParticipant.getVideoTracks().add(remoteVideoTrack);
            remoteParticipant.setState(Participant.State.CONNECTED);
            remoteParticipant.setPinned(false);

            if (VideoCallManagerHolder.getParticipants() == null){
                participants = new ArrayList<>();
            }
            participants.add(remoteParticipant);
            VideoCallManagerHolder.setParticipants(participants);

            binding.callingToInfo.setText("");
            binding.statusCallTv.setText("");
            binding.callingToInfo.setVisibility(View.INVISIBLE);
            binding.statusCallTv.setVisibility(View.INVISIBLE);

            attachRemoteVideo();

        }else if (event instanceof VideoCallEvent.RemoteParticipantEvent.ToggleRemoteAudio){

            VideoCallEvent.RemoteParticipantEvent.ToggleRemoteAudio audio =
                    (VideoCallEvent.RemoteParticipantEvent.ToggleRemoteAudio) event;
            toggleRemoteAudio(audio.isAudioStatus());

        }else if (event instanceof VideoCallEvent.RemoteParticipantEvent.ToggleRemoteVideo){

            VideoCallEvent.RemoteParticipantEvent.ToggleRemoteVideo video =
                    (VideoCallEvent.RemoteParticipantEvent.ToggleRemoteVideo) event;
            toggleRemoteVideo(video.isVideoStatus());

        }else if (event instanceof VideoCallEvent.RemoteParticipantEvent.UserJoined){

            VideoCallEvent.RemoteParticipantEvent.UserJoined userJoined =
                    (VideoCallEvent.RemoteParticipantEvent.UserJoined) event;
            remoteParticipant.setName(userJoined.getName());

            binding.callingToInfo.setText(userJoined.getName());
            binding.statusCallTv.setText("Connected");

        }else if (event instanceof VideoCallEvent.RemoteParticipantEvent.RemoteUserLeave){
            unattachRemoteUser();

        }
    }

    private void handleLocalParticipantEvent(VideoCallEvent.LocalParticipantEvent event){
        if (event instanceof VideoCallEvent.LocalParticipantEvent.LocalVideoTrackUpdated){
            VideoCallEvent.LocalParticipantEvent.LocalVideoTrackUpdated trackUpdated =
                    (VideoCallEvent.LocalParticipantEvent.LocalVideoTrackUpdated) event;

            bindVideoBtn(trackUpdated.isEnable());

            if (trackUpdated.getVideoTrack() != null && trackUpdated.isEnable()){
                localVideoTrack = trackUpdated.getVideoTrack();

                localVideoTrack.removeSink(mainRender.getVideoView());
                localVideoTrack.removeSink(pinRender.getVideoView());

                if (localParticipant.isPinned()){
                    localVideoTrack.addSink(pinRender.getVideoView());
                    pinRender.setMirror(true);

                }else{
                    localVideoTrack.addSink(mainRender.getVideoView());
                    mainRender.setMirror(true);
                }
            }else{
                localVideoTrack.removeSink(mainRender.getVideoView());
                localVideoTrack.removeSink(pinRender.getVideoView());
            }

        }else if (event instanceof VideoCallEvent.LocalParticipantEvent.LocalAudioTrackUpdated){
            VideoCallEvent.LocalParticipantEvent.LocalAudioTrackUpdated trackUpdated =
                    (VideoCallEvent.LocalParticipantEvent.LocalAudioTrackUpdated) event;

            bindMicBtn(trackUpdated.isEnable());

        }
    }

    private void toggleRemoteVideo(boolean status) {
        if (localParticipant.isPinned()){
            if (status){
                mainRender.setState(ParticipantView.State.VIDEO);
            }else{
                mainRender.setState(ParticipantView.State.NO_VIDEO);
            }
        }else{
            if (status){
                pinRender.setState(ParticipantView.State.VIDEO);
            }else{
                pinRender.setState(ParticipantView.State.NO_VIDEO);
            }
        }
    }

    private void toggleRemoteAudio(boolean status) {
        if (localParticipant.isPinned()){
            mainRender.setMuted(status);
            mainRender.setMutedText(remoteParticipant.getName() + " is muted");
        }else{
            pinRender.setMuted(status);
        }
    }

    private void attachRemoteVideo() {
        if (remoteVideoTrack == null) {
            Log.w(TAG, "attachRemoteVideo: remoteVideoTrack es null");
            return;
        }

        remoteVideoTrack.removeSink(mainRender.videoView);
        remoteVideoTrack.removeSink(pinRender.videoView);

        if (localVideoTrack != null) {
            localVideoTrack.removeSink(mainRender.videoView);
            localVideoTrack.removeSink(pinRender.videoView);
            localVideoTrack.addSink(pinRender.videoView);
        }

        remoteVideoTrack.addSink(mainRender.videoView);

        mainRender.setMirror(false);
        pinRender.setMirror(true);

        localParticipant.setPinned(true);

        pinRender.setVisibility(View.VISIBLE);
        mainRender.setVisibility(View.VISIBLE);
    }

    private void clearViews(){
        sessionState.setRoomName("");

        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeSink(mainRender.videoView);
            remoteVideoTrack.removeSink(pinRender.videoView);
        }
        remoteVideoTrack = null;

        pinRender.setVisibility(View.GONE);

        localParticipant.setPinned(false);



    }

    private void unattachRemoteUser(){
        if (remoteVideoTrack != null) {
            remoteVideoTrack.removeSink(mainRender.getVideoView());
            remoteVideoTrack.removeSink(pinRender.getVideoView());
        }
        remoteVideoTrack = null;

        pinRender.setVisibility(View.GONE);
        localParticipant.setPinned(false);
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
    protected void onResume() {
        super.onResume();
        viewModel.processInput(new VideoCallViewEvent.OnResume());
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.processInput(new VideoCallViewEvent.OnPause());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        callingRingtoneManager.stopOutgoingCallProcess();
    }

}
package com.t4app.videocalltest.viewmodel.manager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.t4app.videocalltest.models.LocalParticipant;
import com.t4app.videocalltest.events.VideoCallEvent;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

public class LocalParticipantManager {
    private static final String TAG = "LOCAL_MANAGER";

    private  VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;

    private AudioSource audioSource;
    private AudioTrack localAudioTrack;

    private boolean videoEnable = false;
    private boolean micEnable = false;
    private boolean speakerEnable = true;

    private Context context;
    private VideoCallManager videoCallManager;
    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private LocalParticipant localParticipant;


    public LocalParticipantManager(Context context, VideoCallManager videoCallManager, EglBase eglBase,
                                   PeerConnectionFactory peerConnectionFactory) {
        this.context = context;
        this.videoCallManager = videoCallManager;
        this.eglBase = eglBase;
        this.peerConnectionFactory = peerConnectionFactory;

        localParticipant = new LocalParticipant();
    }

    public void toggleVideo() {
        if (videoEnable) {
            videoEnable = false;
            removeLocalVideoTrack();
        } else {
            videoEnable = true;
            if (localVideoTrack != null) {
                localVideoTrack.setEnabled(true);
                setLocalVideoTrack(localVideoTrack);
            } else {
                setupLocalVideoTrack();
            }
        }
    }

    public void toggleAudio() {
        if (micEnable) {
            micEnable = false;
            removeAudioTrack();
        } else {
            micEnable = true;
            if (localAudioTrack != null) {
                localAudioTrack.setEnabled(true);
                setLocalAudioTrack(localAudioTrack);
            } else {
                setupLocalAudioTrack();
            }
        }
    }

    public void onResume() {
        Log.d(TAG, "IS ENABLE " + videoEnable + " " + micEnable);
        if (videoEnable) {
            setupLocalVideoTrack();
        }
        if (micEnable) {
            new Handler(Looper.getMainLooper()).postDelayed(this::setupLocalAudioTrack, 100);
        }
    }

    public void onPause() {
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(false);
            removeLocalVideoTrack();
        }
    }

    public void setupLocalVideoTrack() {

        Log.d(TAG, "setupLocalVideoTrack()");
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(true);
            videoEnable = true;
            setLocalVideoTrack(localVideoTrack);
            return;
        }

        videoCapturer = createCameraCapturer();
        if (videoCapturer == null) {
            Log.e(TAG, "No CameraCapturer available");
            return;
        }

        if (surfaceTextureHelper == null) {
            surfaceTextureHelper =
                    SurfaceTextureHelper.create("VideoCaptureThread", eglBase.getEglBaseContext());
        }

        if (videoSource == null) {
            videoSource = peerConnectionFactory.createVideoSource(false);
            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());

            try {
                videoCapturer.startCapture(640, 480, 30);
            } catch (Exception e) {
                Log.e(TAG, "Error starting capture", e);
            }
        }

        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video_track", videoSource);
        localVideoTrack.setEnabled(true);
        videoEnable = true;

        Log.d(TAG, "SET LOCAL VIDEO TRACK HERE: ");
        setLocalVideoTrack(localVideoTrack);
    }

    private void setLocalVideoTrack(VideoTrack videoTrack) {
        this.localVideoTrack = videoTrack;
        videoCallManager.sendVideoCallEvent(
                new VideoCallEvent.LocalParticipantEvent.LocalVideoTrackUpdated(videoTrack, videoEnable)
        );
    }

    private void setLocalAudioTrack(AudioTrack audioTrack) {
        this.localAudioTrack = audioTrack;
        videoCallManager.sendVideoCallEvent(
                new VideoCallEvent.LocalParticipantEvent.LocalAudioTrackUpdated(audioTrack, micEnable)
        );
    }

    private void removeLocalVideoTrack() {
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(false);
        }
        setLocalVideoTrack(localVideoTrack);
    }

    public void setupLocalAudioTrack() {
        Log.d(TAG, "setupLocalAudioTrack()");

        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(true);
            micEnable = true;
            setLocalAudioTrack(localAudioTrack);
            return;
        }

        if (audioSource == null) {
            audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        }

        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio_track", audioSource);
        localAudioTrack.setEnabled(true);
        micEnable = true;

        setLocalAudioTrack(localAudioTrack);
    }

    private void removeAudioTrack() {
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(false);
        }
        setLocalAudioTrack(localAudioTrack);
    }

    private void stopAudio() {
        if (audioSource != null) audioSource.dispose();
        audioSource = null;
        localAudioTrack = null;
    }

    private void stopVideo() {
        try {
            if (videoCapturer != null) {
                videoCapturer.stopCapture();
                videoCapturer.dispose();
            }
        } catch (Exception ignored) {}

        if (videoSource != null) videoSource.dispose();
        if (surfaceTextureHelper != null) surfaceTextureHelper.dispose();

        videoCapturer = null;
        videoSource = null;
        surfaceTextureHelper = null;
        localVideoTrack = null;
    }

    private VideoCapturer createCameraCapturer(){
        CameraEnumerator enumerator;
        if (Camera2Enumerator.isSupported(context)){
            enumerator = new Camera2Enumerator(context);
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

    public VideoCapturer getVideoCapturer() {
        return videoCapturer;
    }

    public void setVideoCapturer(VideoCapturer videoCapturer) {
        this.videoCapturer = videoCapturer;
    }

    public SurfaceTextureHelper getSurfaceTextureHelper() {
        return surfaceTextureHelper;
    }

    public void setSurfaceTextureHelper(SurfaceTextureHelper surfaceTextureHelper) {
        this.surfaceTextureHelper = surfaceTextureHelper;
    }

    public VideoSource getVideoSource() {
        return videoSource;
    }

    public void setVideoSource(VideoSource videoSource) {
        this.videoSource = videoSource;
    }

    public VideoTrack getLocalVideoTrack() {
        return localVideoTrack;
    }

    public AudioSource getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(AudioSource audioSource) {
        this.audioSource = audioSource;
    }

    public AudioTrack getLocalAudioTrack() {
        return localAudioTrack;
    }


    public boolean isVideoEnable() {
        return videoEnable;
    }

    public void setVideoEnable(boolean videoEnable) {
        this.videoEnable = videoEnable;
    }

    public boolean isMicEnable() {
        return micEnable;
    }

    public void setMicEnable(boolean micEnable) {
        this.micEnable = micEnable;
    }

    public boolean isSpeakerEnable() {
        return speakerEnable;
    }

    public void setSpeakerEnable(boolean speakerEnable) {
        this.speakerEnable = speakerEnable;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public VideoCallManager getVideoCallManager() {
        return videoCallManager;
    }

    public void setVideoCallManager(VideoCallManager videoCallManager) {
        this.videoCallManager = videoCallManager;
    }

    public EglBase getEglBase() {
        return eglBase;
    }

    public void setEglBase(EglBase eglBase) {
        this.eglBase = eglBase;
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return peerConnectionFactory;
    }

    public void setPeerConnectionFactory(PeerConnectionFactory peerConnectionFactory) {
        this.peerConnectionFactory = peerConnectionFactory;
    }
}

package com.t4app.t4videocall.events;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.t4app.t4videocall.R;
import com.t4app.t4videocall.ui.VideoCallActivity;

public class CallingRingtoneManager {

    private MediaPlayer outgoingPlayer;
    private Vibrator vibrator;
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private VideoCallActivity context;

    public CallingRingtoneManager(VideoCallActivity context) {
        this.context = context;
    }

    public void startOutgoingCallProcess() {
        startOutgoingCallTone();
        startVibrationPattern();
        startCallTimeout();
    }

    public void startOutgoingCallTone() {
        try {
            outgoingPlayer = new MediaPlayer();
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.outgoing_ringtone);
            outgoingPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            outgoingPlayer.setAudioStreamType(AudioManager.STREAM_RING);
            outgoingPlayer.setLooping(true);
            outgoingPlayer.prepare();
            outgoingPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopOutgoingCallTone() {
        if (outgoingPlayer != null) {
            try {
                outgoingPlayer.stop();
                outgoingPlayer.release();
            } catch (Exception ignored) {}
            outgoingPlayer = null;
        }
    }

    private void startVibrationPattern() {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;

        long[] pattern = {0, 300, 800};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopVibration() {
        if (vibrator != null) vibrator.cancel();
    }

    private void startCallTimeout() {
        timeoutRunnable = () -> {
            stopOutgoingCallProcess();
            context.finish();
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30_000);
    }

    private void cancelCallTimeout() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
    }

    public void stopOutgoingCallProcess() {
        stopOutgoingCallTone();
        stopVibration();
        cancelCallTimeout();
    }

    private void startCall() {
        startOutgoingCallProcess();
    }

    private void onCallAccepted() {
        stopOutgoingCallProcess();
    }

    private void onCallRejected() {
        stopOutgoingCallProcess();
        context.finish();
    }


}

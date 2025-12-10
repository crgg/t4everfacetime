package com.t4app.videocalltest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.t4app.videocalltest.viewmodel.manager.VideoCallManager;
import com.t4app.videocalltest.viewmodel.VideoCallManagerHolder;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VideoService extends Service {

    private static final String ROOM_NAME_EXTRA = "ROOM_NAME_EXTRA";

    private static final int ONGOING_NOTIFICATION_ID = 1001;

    public static boolean isServiceStarted = false;

    public static VideoService videoService = null;

    @javax.inject.Inject
    VideoCallManager videoCallManager;

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        setupForegroundService(intent);

        isServiceStarted = true;
        videoService = this;


        VideoCallManagerHolder.set(videoCallManager);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);

        isServiceStarted = false;
        videoService = null;
        VideoCallManagerHolder.set(null);
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        return null;
    }

    public void enableScreenShare(String roomName, boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            VideoCallNotification roomNotification = new VideoCallNotification(this);

            int serviceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                    | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;

            if (enable) {
                serviceTypes |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            }

            startForeground(
                    ONGOING_NOTIFICATION_ID,
                    roomNotification.buildNotification(roomName),
                    serviceTypes
            );
        }
    }

    private void setupForegroundService(@Nullable Intent intent) {
        if (intent == null) return;

        String roomName = intent.getStringExtra(ROOM_NAME_EXTRA);
        if (roomName == null) return;

        VideoCallNotification roomNotification = new VideoCallNotification(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                    ONGOING_NOTIFICATION_ID,
                    roomNotification.buildNotification(roomName),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                            | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(
                    ONGOING_NOTIFICATION_ID,
                    roomNotification.buildNotification(roomName)
            );
        }
    }


    public static void startService(Context context, String roomName) {
        Intent intent = new Intent(context, VideoService.class);
        intent.putExtra(ROOM_NAME_EXTRA, roomName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, VideoService.class);
        context.stopService(intent);
    }

    public VideoCallManager getRoomManager() {
        return videoCallManager;
    }

    public static boolean isRunning() {
        return isServiceStarted && videoService != null;
    }
}

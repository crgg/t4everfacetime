package com.t4app.videocalltest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.t4app.videocalltest.ui.VideoCallActivity;

public class VideoCallNotification {

    private static final String VIDEO_SERVICE_CHANNEL = "VIDEO_SERVICE_CHANNEL";

    public static final int ONGOING_NOTIFICATION_ID = 1;

    private final Context context;

    public VideoCallNotification(Context context) {
        this.context = context;

        createDownloadNotificationChannel(
                VIDEO_SERVICE_CHANNEL,
                context.getString(R.string.room_notification_channel_title),
                context
        );
    }

    private PendingIntent getPendingIntent() {
        Intent notificationIntent = new Intent(context, VideoCallActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                flags
        );
    }

    public Notification buildNotification(String roomName) {

        return new NotificationCompat.Builder(context, VIDEO_SERVICE_CHANNEL)
                .setContentTitle(context.getString(R.string.room_notification_title, roomName))
                .setContentText(context.getString(R.string.notification_message))
                .setContentIntent(getPendingIntent())
                .setUsesChronometer(true)
                .setSmallIcon(R.drawable.ic_video_cam)
                .setTicker(context.getString(R.string.notification_message))
                .build();
    }

    private void createDownloadNotificationChannel(
            String channelId,
            String channelName,
            Context context
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

package com.t4app.t4videocall;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.t4app.t4videocall.ui.IncomingCallActivity;
import com.t4app.t4videocall.ui.VideoCallActivity;

public class FirebaseService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseService";
    private static final String CHANNEL_ID = "incoming_call_channel";
    public static final int NOTIFICATION_ID = 999;

    public static final String ACTION_DECLINE_CALL =
            "com.t4app.t4tmsdispatchandroid.ACTION_DECLINE_CALL";

    private static Ringtone ringtone;

    private static FirebaseService instance;


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.d(TAG, "Nuevo token FCM generado: " + token);

        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.setDeviceToken(token);
        copyToClipboard(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().isEmpty()) return;

        String type = remoteMessage.getData().get("type");

        if ("incoming_call".equals(type)) {
            String caller = remoteMessage.getData().get("caller");
            String room = remoteMessage.getData().get("room_name");

            Log.d(TAG, "Llamada entrante de: " + caller + " sala: " + room);

            if (isDeviceLocked()) {
                showFullScreenIncomingCall(caller, room);
            } else {
                showHeadsUpNotification(caller, room);
            }
        }
    }

    private void showFullScreenIncomingCall(String caller, String room) {
        startRingtone();
        Intent intent = new Intent(this, IncomingCallActivity.class);
        intent.putExtra("caller", caller);
        intent.putExtra("room_name", room);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent fullScreenPending =
                PendingIntent.getActivity(
                        this,
                        100,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        Intent intentAccept = new Intent(this, VideoCallActivity.class);
        intentAccept.putExtra("room_name", room);
        intentAccept.putExtra("is_incoming", true);
        intentAccept.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent acceptPending = PendingIntent.getActivity(
                this,
                201,
                intentAccept,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent declineIntent = new Intent(this, DeclineReceiver.class);
        declineIntent.setAction(ACTION_DECLINE_CALL);
        declineIntent.putExtra("room_name", room);
        PendingIntent declinePending =
                PendingIntent.getBroadcast(
                        this,
                        101,
                        declineIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        createNotificationChannel();

        @SuppressLint("FullScreenIntentPolicy") NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_call)
                        .setContentTitle(getString(R.string.incoming_call))
                        .setContentText(getString(R.string.from) + caller)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setOngoing(true)
                        .addAction(R.drawable.ic_call, getString(R.string.accept), acceptPending)
                        .addAction(R.drawable.ic_call_end, getString(R.string.decline), declinePending)
                        .setFullScreenIntent(fullScreenPending, true)
                        .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        manager.notify(NOTIFICATION_ID, builder.build());
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
        }, 30000);
    }

    private void showHeadsUpNotification(String caller, String room) {
        startRingtone();

        Intent intentAccept = new Intent(this, VideoCallActivity.class);
        intentAccept.putExtra("room_name", room);
        intentAccept.putExtra("is_incoming", true);
        intentAccept.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent intent = new Intent(this, IncomingCallActivity.class);
        intent.putExtra("caller", caller);
        intent.putExtra("room_name", room);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent fullScreenPending =
                PendingIntent.getActivity(
                        this,
                        100,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        PendingIntent acceptPending =
                PendingIntent.getActivity(
                        this,
                        200,
                        intentAccept,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        Intent declineIntent = new Intent(this, DeclineReceiver.class);
        declineIntent.setAction(ACTION_DECLINE_CALL);
        declineIntent.putExtra("room_name", room);

        PendingIntent declinePending =
                PendingIntent.getBroadcast(
                        this,
                        201,
                        declineIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        createNotificationChannel();

        @SuppressLint("FullScreenIntentPolicy") NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_call)
                        .setContentTitle(getString(R.string.incoming_call))
                        .setContentText(getString(R.string.from) + caller)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setOngoing(true)
                        .setFullScreenIntent(fullScreenPending, true)
                        .addAction(R.drawable.ic_call, getString(R.string.accept), acceptPending)
                        .addAction(R.drawable.ic_call_end, getString(R.string.decline), declinePending)
                        .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        manager.notify(NOTIFICATION_ID, builder.build());

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
        }, 30000);
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.incoming);

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(getString(R.string.incoming_call_notification));
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            channel.setSound(soundUri, attributes);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void copyToClipboard(String token) {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText("FCM Token", token);
        clipboard.setPrimaryClip(clip);

        Log.d(TAG, "Token FCM copiado al portapapeles.");
    }

    public boolean isDeviceLocked() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }

    private void startRingtone() {
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.incoming);

        ringtone = RingtoneManager.getRingtone(getApplicationContext(), soundUri);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setLooping(true);
        }

        ringtone.play();
    }

    public void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    public static FirebaseService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }
}
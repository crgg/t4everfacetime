package com.t4app.t4videocall.ui;

import static com.t4app.t4videocall.FirebaseService.NOTIFICATION_ID;

import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.t4app.t4videocall.FirebaseService;
import com.t4app.t4videocall.R;

public class IncomingCallActivity extends AppCompatActivity {

    public static boolean isOpen = false;
    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incoming_call);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        FirebaseService service = FirebaseService.getInstance();
        if (service != null){
            service.stopRingtone();
        }

        isOpen = true;

        startRingtone();
        setVolumeControlStream(AudioManager.STREAM_RING);

        String caller = getIntent().getStringExtra("caller");
        String room = getIntent().getStringExtra("room_name");

        TextView callerText = findViewById(R.id.callerText);
        callerText.setText(caller);

        findViewById(R.id.btnAccept).setOnClickListener(v -> {
            stopRingtone();
            Intent i = new Intent(this, VideoCallActivity.class);
            i.putExtra("room_name", room);
            i.putExtra("is_incoming", true);
            startActivity(i);
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
            finish();
        });

        findViewById(R.id.btnDecline).setOnClickListener(v -> {
            stopRingtone();
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);

            finish();
        });
    }

    private void startRingtone() {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.setLooping(true);
            }

            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
        isOpen = false;
    }
}
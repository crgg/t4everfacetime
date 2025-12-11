package com.t4app.t4videocall;

import static com.t4app.t4videocall.FirebaseService.NOTIFICATION_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

public class DeclineReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String room = intent.getStringExtra("room_name");
        Log.d("DeclineReceiver", "Llamada rechazada para sala: " + room);
        FirebaseService service = FirebaseService.getInstance();
        if (service != null){
            service.stopRingtone();
        }

        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID);

    }
}

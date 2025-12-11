package com.t4app.t4videocall;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class PermissionUtil {

    private final Context context;

    public PermissionUtil(Context context) {
        this.context = context;
    }

    public boolean isPermissionGranted(String permission) {
        return ActivityCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}

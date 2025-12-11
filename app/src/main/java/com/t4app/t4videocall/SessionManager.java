package com.t4app.t4videocall;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static SessionManager instance;
    private static Context context;
    private static final String SHARED_PREFS_NAME = "user_session_prefs";
    private static final String KEY_DEVICE_TOKEN = "device_token";
    private SharedPreferences.Editor editor;

    private SessionManager(Context ctx) {
        context = ctx.getApplicationContext();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UserSessionManager not initialized. Call initialize(context) first.");
        }
        return instance;
    }

    public static synchronized void initialize(Context ctx) {
        if (instance == null) {
            instance = new SessionManager(ctx);
        }
    }


    public String getDeviceToken() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_DEVICE_TOKEN, null);
    }

    public void setDeviceToken(String deviceToken) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString(KEY_DEVICE_TOKEN , deviceToken);
        editor.apply();
    }

    public void clearSession() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

}

package com.t4app.t4videocall;

import android.app.Application;

import com.google.firebase.FirebaseApp;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class AppController extends Application {

    public AppController() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        SessionManager.initialize(getApplicationContext());
    }
}

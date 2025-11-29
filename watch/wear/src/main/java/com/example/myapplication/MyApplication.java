package com.example.myapplication;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        // Enable Firebase Realtime Database persistence
//            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
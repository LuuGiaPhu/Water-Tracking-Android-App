package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class NotificationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Close the activity after a delay
        new Handler().postDelayed(() -> finish(), 6000); // Close after 3 seconds
    }
}
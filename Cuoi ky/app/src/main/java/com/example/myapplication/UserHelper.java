package com.example.myapplication;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class UserHelper {

    public static String createUserId(GoogleSignInAccount account) {
        if (account != null) {
            // Use the Google account ID as the user ID
            return account.getId();
        } else {
            throw new IllegalArgumentException("GoogleSignInAccount is null");
        }
    }
}
package com.example.myapplication;

        import android.content.Intent;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.ImageView;
        import android.widget.TextView;

        import androidx.appcompat.app.AppCompatActivity;

        import com.google.android.gms.auth.api.signin.GoogleSignIn;
        import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
        import com.google.android.gms.auth.api.signin.GoogleSignInClient;
        import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
        import com.google.android.gms.common.api.ApiException;
        import com.google.android.gms.tasks.Task;
        import com.squareup.picasso.Picasso;

        public class SignInActivity extends AppCompatActivity {

            private static final int RC_SIGN_IN = 9001;
            private GoogleSignInClient mGoogleSignInClient;
            private ImageView avatarImageView;
            private Button signInButton;

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_sign_in);

                // Initialize views
                avatarImageView = findViewById(R.id.avatarImageView);
                signInButton = findViewById(R.id.signInButton);

                // Configure Google Sign-In
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();
                mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

                // Set click listener for sign-in
                signInButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("SignInActivity", "Sign in button clicked");
                        signIn();
                    }
                });

                // Check for existing Google Sign-In account
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                if (account != null) {
                    updateUI(account);
                }
            }

            private void signIn() {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }

            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);

                if (requestCode == RC_SIGN_IN) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleSignInResult(task);
                }
            }

            private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
                try {
                    GoogleSignInAccount account = completedTask.getResult(ApiException.class);
                    updateUI(account);
                } catch (ApiException e) {
                    Log.w("SignInActivity", "signInResult:failed code=" + e.getStatusCode());
                }
            }

            private void updateUI(GoogleSignInAccount account) {
                if (account != null) {
                    String personName = account.getDisplayName();
                    String personPhotoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;

                    signInButton.setText(personName);
                    if (personPhotoUrl != null) {
                        Picasso.get().load(personPhotoUrl).into(avatarImageView);
                    }

                    // Navigate to MainActivity
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }
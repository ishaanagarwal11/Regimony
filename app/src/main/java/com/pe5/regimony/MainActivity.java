package com.pe5.regimony;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.common.SignInButton;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";  // Debug tag for logging
    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    ShapeableImageView imageView;
    TextView name, mail;
    Button logoutButton;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "Sign-In result received, resultCode: " + result.getResultCode());

                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "Google Sign-In completed successfully");
                        Intent data = result.getData();
                        if (data != null) {
                            Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                                Log.d(TAG, "Sign-In account details retrieved, attempting Firebase authentication");

                                // Proceed with Firebase authentication
                                AuthCredential credential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                                auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Firebase authentication successful");
                                            FirebaseUser user = auth.getCurrentUser();
                                            if (user != null) {
                                                // User is signed in, go to Home activity
                                                goToHomePage();
                                            }
                                        } else {
                                            Log.e(TAG, "Firebase authentication failed", task.getException());
                                        }
                                    }
                                });
                            } catch (ApiException e) {
                                Log.e(TAG, "Google Sign-In failed", e);
                            }
                        }
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Log.e(TAG, "Sign-in canceled by user");
                        Toast.makeText(MainActivity.this, "Sign-in canceled", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Sign-in failed with resultCode: " + result.getResultCode());
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        imageView = findViewById(R.id.profileImage);
        name = findViewById(R.id.nameTV);
        mail = findViewById(R.id.mailTV);
        logoutButton = findViewById(R.id.logoutButton);  // Reference to the Logout button

        // Hide logout button initially
        logoutButton.setVisibility(View.GONE);

        // Check if the user is already signed in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, redirect to Home
            goToHomePage();
        }

        // Google Sign-In Options
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))  // Ensure your client_id is set correctly in strings.xml
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(MainActivity.this, options);

        // Sign-In Button
        SignInButton signInButton = findViewById(R.id.signIn);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Sign-In button clicked");
                Intent signInIntent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(signInIntent);
                Log.d(TAG, "Sign-In intent launched, waiting for user interaction");
            }
        });
    }

    // Redirect to Home activity
    private void goToHomePage() {
        Intent intent = new Intent(MainActivity.this, Home.class);
        startActivity(intent);
        finish(); // Close MainActivity
    }
}

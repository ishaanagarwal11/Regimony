package com.pe5.regimony;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.imageview.ShapeableImageView;

public class Profile extends AppCompatActivity {

    private static final int REQUEST_CALL_PERMISSION = 1;  // Request code for call permission
    private static final String EMERGENCY_NUMBER = "911";  // Replace with the emergency number

    private CustomCircularImageView profileImage;
    private TextView profileName, profileEmail;
    private Button logoutButton, emergencyCallButton;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance();

        // Initialize GoogleSignInClient
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // Initialize UI elements
// Corrected line for the custom circular image view
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        logoutButton = findViewById(R.id.logoutButton);
        emergencyCallButton = findViewById(R.id.emergencyCallButton);

        // Get the current logged-in user
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            // Display user's profile information
            profileName.setText(user.getDisplayName());
            profileEmail.setText(user.getEmail());

            // Load user profile image
            Glide.with(this).load(user.getPhotoUrl()).into(profileImage);

            // Make the logout button visible
            logoutButton.setVisibility(View.VISIBLE);
        }

        // Set up logout button action
        logoutButton.setOnClickListener(view -> {
            // Sign out of Firebase and Google
            auth.signOut();
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // After signing out, navigate to MainActivity for sign-in
                Intent intent = new Intent(Profile.this, MainActivity.class);
                startActivity(intent);
                finish();  // Close Profile activity
            });
        });

        // Setup BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                Intent intent = new Intent(Profile.this, Home.class); // Navigate to Home activity
                startActivity(intent);
                finish();  // Close current Profile activity
                return true;
            } else if (id == R.id.navigation_daily) {
                Intent intent = new Intent(Profile.this, Daily.class); // Navigate to Daily activity
                startActivity(intent);
                finish();  // Close current Profile activity
                return true;
            } else if (id == R.id.navigation_records) {
                Intent intent = new Intent(Profile.this, Records.class); // Navigate to Records activity
                startActivity(intent);
                finish();  // Close current Profile activity
                return true;
            } else if (id == R.id.navigation_profile) {
                // Already in Profile activity, do nothing
                return true;
            }

            return false;
        });

        // Set up emergency call button
        emergencyCallButton.setOnClickListener(v -> makeEmergencyCall());
    }

    // Method to check permission and initiate a call
    private void makeEmergencyCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Request CALL_PHONE permission if it's not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PERMISSION);
        } else {
            // If permission is already granted, make the call
            startCallIntent();
        }
    }

    // Method to start the dialer or place a call
    private void startCallIntent() {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + EMERGENCY_NUMBER));
        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            Toast.makeText(this, "Call permission required", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle the permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCallIntent(); // Permission granted, start the call
            } else {
                Toast.makeText(this, "Permission DENIED to make calls", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

package com.pe5.regimony;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.Random;

public class Home extends AppCompatActivity {

    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    Button logoutButton;
    TextView greetingText;
    LinearLayout webviewContainer;
    ScrollView scrollView;

    // Video embed codes array
    private String[] videoEmbeds = {
            "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/V2KCAfHjySQ?si=L6JgLFvk7zzqT39S\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>",
            "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/3dQ6yKSttEc?si=D3H6jti4v7pRAAqQ\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>",
            "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/R0GO9vdYvec?si=TyABzHuGfsYiN4XX\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>"
    };

    Random random = new Random(); // To randomly pick videos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();

        // Configure Google SignIn options
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(Home.this, options);

        greetingText = findViewById(R.id.greetingText);
        logoutButton = findViewById(R.id.logoutButton);
        webviewContainer = findViewById(R.id.webviewContainer);
        scrollView = findViewById(R.id.scrollView);

        // Set greeting message based on the time of the day
        setGreetingMessage();

        // Set logout button action
        logoutButton.setOnClickListener(view -> {
            // Sign out from Firebase and Google
            auth.signOut();
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                // Redirect to MainActivity after signing out
                Intent intent = new Intent(Home.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close Home activity
            });
        });

        // Load initial videos
        loadInitialVideos();

        // Infinite scrolling: load more videos when reaching the bottom
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (scrollView.getChildAt(0).getBottom() <= (scrollView.getHeight() + scrollView.getScrollY())) {
                loadMoreVideos(); // Load more videos when scrolled to the bottom
            }
        });

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the selected item to Home
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // Handle bottom navigation item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                // You're already in the Home activity, do nothing
                return true;
            } else if (id == R.id.navigation_daily) {
                Intent intent = new Intent(Home.this, Daily.class); // Navigate to Daily activity
                startActivity(intent);
                finish();  // Close current Home activity
                return true;
            } else if (id == R.id.navigation_records) {
                Intent intent = new Intent(Home.this, Records.class); // Navigate to Records activity
                startActivity(intent);
                finish();  // Close current Home activity
                return true;
            } else if (id == R.id.navigation_profile) {
                Intent intent = new Intent(Home.this, Profile.class); // Navigate to Profile activity
                startActivity(intent);
                finish();  // Close current Home activity
                return true;
            }

            return false;
        });

    }

    // Load initial set of videos
    private void loadInitialVideos() {
        for (int i = 0; i < 3; i++) {
            addRandomVideoEmbed();
        }
    }

    // Load more videos when the user scrolls
    private void loadMoreVideos() {
        for (int i = 0; i < 3; i++) {
            addRandomVideoEmbed();
        }
    }

    // Adds a randomly chosen YouTube embed to the LinearLayout
    private void addRandomVideoEmbed() {
        WebView webView = new WebView(this);

        // Get the device's screen width
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        // Set up margins for the WebView (You can adjust margin values)
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,  // Make the WebView take up full width
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Optional: Add margins if needed (in pixels)
        int marginInDp = 16;  // Adjust if needed
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
        params.setMargins(marginInPx, 0, marginInPx, 30); // Apply margins

        // Apply the layout params to the WebView
        webView.setLayoutParams(params);

        // Ensure iframe fits the screen width using 100% width and maintains aspect ratio
        String videoHtml = "<html><body style='margin:0;padding:0;'>"
                + "<iframe width=\"100%\" height=\"auto\" style=\"aspect-ratio: 16/9;\""
                + " src=\"https://www.youtube.com/embed/V2KCAfHjySQ?si=L6JgLFvk7zzqT39S\""
                + " frameborder=\"0\" allowfullscreen></iframe>"
                + "</body></html>";

        // Adjust this to randomly pick one of your video embeds
        String randomVideoEmbed = videoEmbeds[random.nextInt(videoEmbeds.length)];

        // Load the responsive HTML into the WebView
        webView.loadData(videoHtml, "text/html", "utf-8");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());

        // Add the WebView to the container
        webviewContainer.addView(webView);
    }

    private void setGreetingMessage() {
        String greeting;
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        if (hourOfDay >= 0 && hourOfDay < 12) {
            greeting = "Good Morning";
        } else if (hourOfDay >= 12 && hourOfDay < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }

        // If the user is logged in, include their name in the greeting
        if (auth.getCurrentUser() != null) {
            greeting += ", " + auth.getCurrentUser().getDisplayName();
        }

        greetingText.setText(greeting);
    }
}

package com.pe5.regimony;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Home extends AppCompatActivity {

    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    TextView greetingText, healthTipText;
    LinearLayout webviewContainer;
    ScrollView scrollView;
    SwipeRefreshLayout swipeRefreshLayout;

    // Hardcoded YouTube embed codes
    private String[] videoEmbeds = {
            "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/c06dTj0v0sM?si=ZxE-ShVEMymfsfsZ\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>",
            "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/Y8HIFRPU6pM?si=r1YCu7Ig10eVb4Lw\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>",
            "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/vYC5ZzJI2PU?si=ad_ouYp_u-4DZxSp\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>",
            // Add more hardcoded video embeds as needed
    };

    Random random = new Random(); // Random object for video shuffling

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
        healthTipText = findViewById(R.id.healthTipText);
        webviewContainer = findViewById(R.id.webviewContainer);
        scrollView = findViewById(R.id.scrollView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout); // Pull to refresh

        // Set greeting message based on the time of the day
        setGreetingMessage();

        // Fetch health tip from Gemini API
        fetchHealthTip();

        // Initial loading of videos
        loadContent();

        // Swipe down to refresh functionality
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadContent(); // Load and shuffle videos
            fetchHealthTip(); // Fetch a new health tip
            swipeRefreshLayout.setRefreshing(false); // Stop the refresh indicator
        });

        // Infinite scrolling: load more videos when reaching the bottom
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (scrollView.getChildAt(0).getBottom() <= (scrollView.getHeight() + scrollView.getScrollY())) {
                loadMoreVideos(); // Load more videos when scrolled to the bottom
            }
        });

        // Set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                return true; // Already in Home
            } else if (id == R.id.navigation_daily) {
                startActivity(new Intent(Home.this, Daily.class));
                finish();
                return true;
            } else if (id == R.id.navigation_records) {
                startActivity(new Intent(Home.this, Records.class));
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                startActivity(new Intent(Home.this, Profile.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadContent() {
        // Shuffle and load initial set of videos
        List<String> shuffledVideos = new ArrayList<>();
        Collections.addAll(shuffledVideos, videoEmbeds);
        Collections.shuffle(shuffledVideos); // Shuffle videos

        webviewContainer.removeAllViews(); // Clear previous videos
        for (int i = 0; i < 3; i++) { // Load three videos initially
            addVideoEmbed(shuffledVideos.get(i));
        }
    }

    private void loadMoreVideos() {
        List<String> shuffledVideos = new ArrayList<>();
        Collections.addAll(shuffledVideos, videoEmbeds);
        Collections.shuffle(shuffledVideos);

        for (int i = 0; i < 3; i++) {
            addVideoEmbed(shuffledVideos.get(i));
        }
    }

    private void addVideoEmbed(String videoEmbed) {
        WebView webView = new WebView(this);

        // Get screen width and calculate height for a 16:9 aspect ratio
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int videoHeight = screenWidth * 9 / 16;

        // Set up layout parameters with calculated height
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,  // Match screen width
                videoHeight  // Maintain 16:9 aspect ratio
        );

        int marginInDp = 8;  // Adjust margins as needed
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        webView.setLayoutParams(params);

        // Load the HTML content into the WebView with responsive iframe style
        String videoHtml = "<html><body style='margin:0;padding:0;'>" +
                "<iframe width=\"100%\" height=\"100%\" style=\"aspect-ratio:16/9;\" src=\"" +
                extractVideoSrc(videoEmbed) + "\" frameborder=\"0\" allowfullscreen></iframe>" +
                "</body></html>";

        webView.loadData(videoHtml, "text/html", "utf-8");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webviewContainer.addView(webView);
    }

    // Helper method to extract the src URL from the embed code
    private String extractVideoSrc(String embedCode) {
        int start = embedCode.indexOf("src=\"") + 5;
        int end = embedCode.indexOf("\"", start);
        return embedCode.substring(start, end);
    }


    private void fetchHealthTip() {
        // Fetch a health tip using GeminiAPIHelper
        String prompt = "Provide a health tip in a single sentence.";
        GeminiAPIHelper.fetchDataFromGemini(prompt, new GeminiAPIHelper.GeminiAPIResponse() {
            @Override
            public void onResult(String responseText) {
                if (responseText != null) {
                    healthTipText.setText(responseText); // Display the health tip
                } else {
                    healthTipText.setText("Stay healthy!"); // Fallback message
                }
            }
        });
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

        if (auth.getCurrentUser() != null) {
            greeting += "!";
        }

        greetingText.setText(greeting);
    }
}

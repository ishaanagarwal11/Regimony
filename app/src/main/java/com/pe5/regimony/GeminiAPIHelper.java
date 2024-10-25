package com.pe5.regimony;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeminiAPIHelper {

    private static final String API_KEY = "AIzaSyAiHoMeEKL0kBhB_2jomZU1he5kgs2D3rI";  // Replace with your actual API key
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + API_KEY;

    public interface GeminiAPIResponse {
        void onResult(String responseText);
    }

    public static void fetchDataFromGemini(String prompt, GeminiAPIResponse responseCallback) {
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... voids) {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    // API request payload
                    JSONObject requestPayload = new JSONObject();
                    requestPayload.put("contents", new JSONArray().put(new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))));

                    // Write the request payload
                    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(requestPayload.toString());
                    writer.flush();

                    // Read the response
                    StringBuilder response = new StringBuilder();
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        java.io.InputStreamReader reader = new java.io.InputStreamReader(conn.getInputStream());
                        int data;
                        while ((data = reader.read()) != -1) {
                            response.append((char) data);
                        }
                        reader.close();
                    }

                    return new JSONObject(response.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                if (result != null) {
                    try {
                        // Extract the text content from the response
                        String text = result.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        // Pass the response text to the callback
                        responseCallback.onResult(text);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("GeminiAPI", "Failed to retrieve response");
                    responseCallback.onResult(null);
                }
            }
        }.execute();
    }
}

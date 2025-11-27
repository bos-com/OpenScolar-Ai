package com.example.scolarai;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiClient {

    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface Callback {
        void onResponse(String response);
        void onError(String error);
    }

    public static void sendPrompt(String prompt, Callback callback) {
        new Thread(() -> {
            try {
                // Build the JSON structure matching Gemini API format
                JSONObject json = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();

                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                json.put("contents", contents);

                RequestBody body = RequestBody.create(json.toString(), JSON);

                Request request = new Request.Builder()
                        .url(ENDPOINT)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("X-goog-api-key", BuildConfig.GEMINI_API_KEY)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        // Parse the Gemini API response
                        JSONObject responseJson = new JSONObject(respBody);
                        JSONArray candidates = responseJson.optJSONArray("candidates");

                        if (candidates != null && candidates.length() > 0) {
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject contentObj = candidate.getJSONObject("content");
                            JSONArray partsArray = contentObj.getJSONArray("parts");
                            String text = partsArray.getJSONObject(0).getString("text");

                            // Run callback on main thread
                            new Handler(Looper.getMainLooper()).post(() -> callback.onResponse(text));
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError("No response from Gemini"));
                        }
                    } else {
                        String errorMsg = "Error: " + response.code() + " - " + respBody;
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(errorMsg));
                    }
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(errorMsg));
            }
        }).start();
    }
}
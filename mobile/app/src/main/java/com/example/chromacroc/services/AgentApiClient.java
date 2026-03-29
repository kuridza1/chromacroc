package com.example.chromacroc.services;

import android.os.Build;
import android.util.Base64;

import com.example.chromacroc.model.ColorBlindnessType;

import org.json.*;
import okhttp3.*;
import java.io.File;
import java.nio.file.Files;

public class AgentApiClient {

    private static final String BASE_URL = "https://elenore-nonfervent-velda.ngrok-free.dev";
    private static final String APP_NAME = "chromaCrocAgents";
    private static final String USER_ID  = "user1";
    private static final String SESSION_ID = "session1";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(250, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    public interface Callback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    /**
     * @param imageFile  photo to analyse
     * @param question   free-text question from the user (may be empty)
     * @param type       selected color blindness type — injected into the prompt
     * @param callback   result
     */
    public static void ask(File imageFile,
                           String question,
                           ColorBlindnessType type,
                           Callback callback) {
        new Thread(() -> {
            try {
                // Step 1: create / reuse session
                Request sessionRequest = new Request.Builder()
                        .url(BASE_URL + "/apps/" + APP_NAME + "/users/" + USER_ID + "/sessions/" + SESSION_ID)
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .post(RequestBody.create("{}", MediaType.parse("application/json")))
                        .build();
                client.newCall(sessionRequest).execute().close();

                // Step 2: build prompt that includes the selected type
                String prompt = buildPrompt(question, type);

                // Step 3: encode image
                byte[] imageBytes = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    imageBytes = Files.readAllBytes(imageFile.toPath());
                }
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                JSONObject inlineData = new JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", base64Image);

                JSONObject imagePart = new JSONObject().put("inline_data", inlineData);
                JSONObject textPart  = new JSONObject().put("text", prompt);

                JSONObject newMessage = new JSONObject()
                        .put("role", "user")
                        .put("parts", new JSONArray().put(imagePart).put(textPart));

                JSONObject body = new JSONObject()
                        .put("app_name", APP_NAME)
                        .put("user_id", USER_ID)
                        .put("session_id", SESSION_ID)
                        .put("new_message", newMessage);

                android.util.Log.d("AgentAPI", "Sending body: " + body.toString(2));

                Request request = new Request.Builder()
                        .url(BASE_URL + "/run")
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    android.util.Log.d("AgentAPI", "Status: " + response.code());
                    android.util.Log.d("AgentAPI", "Response: " + responseBody);

                    if (!response.isSuccessful()) {
                        callback.onFailure("Server error " + response.code() + ": " + responseBody);
                        return;
                    }

                    callback.onSuccess(parseAgentResponse(responseBody));
                }

            } catch (Exception e) {
                android.util.Log.e("AgentAPI", "Exception: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }
        }).start();
    }

    // -----------------------------------------------------------------------
    // Prompt construction
    // -----------------------------------------------------------------------

    private static String buildPrompt(String userQuestion, ColorBlindnessType type) {
        String typeLabel = typeLabel(type);
        String base = "The user has " + typeLabel + ". "
                + "Please analyse this image for someone with " + typeLabel + ". "
                + "Skip asking which type of colour blindness they have — it is already known.";

        if (userQuestion != null && !userQuestion.trim().isEmpty()) {
            return base + " Additional question from the user: " + userQuestion.trim();
        }
        return base;
    }

    private static String typeLabel(ColorBlindnessType type) {
        if (type == null) return "an unspecified form of colour blindness";
        switch (type) {
            case PROTANOPIA:    return "protanopia (red-blindness)";
            case DEUTERANOPIA:  return "deuteranopia (green-blindness)";
            case TRITANOPIA:    return "tritanopia (blue-yellow blindness)";
            case ACHROMATOPSIA: return "achromatopsia (complete colour blindness)";
            default:            return "an unspecified form of colour blindness";
        }
    }

    // -----------------------------------------------------------------------
    // Response parsing
    // -----------------------------------------------------------------------

    private static String parseAgentResponse(String json) {
        android.util.Log.d("AgentAPI", "Raw response: " + json);
        try {
            JSONArray events = new JSONArray(json);
            for (int i = events.length() - 1; i >= 0; i--) {
                JSONObject event = events.getJSONObject(i);
                if (!event.has("content")) continue;
                JSONArray parts = event.getJSONObject("content").optJSONArray("parts");
                if (parts == null) continue;
                for (int j = 0; j < parts.length(); j++) {
                    String text = parts.getJSONObject(j).optString("text", "");
                    if (!text.isEmpty()) return text;
                }
            }
        } catch (JSONException e) {
            android.util.Log.e("AgentAPI", "Parse error: " + e.getMessage());
        }
        return "No response from agent.";
    }
}
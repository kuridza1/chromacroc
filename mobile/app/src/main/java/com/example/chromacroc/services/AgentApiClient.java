package com.example.chromacroc.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import com.example.chromacroc.model.ColorBlindnessType;

import org.json.*;
import okhttp3.*;
import okio.BufferedSource;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class AgentApiClient {

    private static final String TAG = "AgentAPI";

    private static final String BASE_URL   = "https://elenore-nonfervent-velda.ngrok-free.dev";
    private static final String APP_NAME   = "chromaCrocAgents";
    private static final String USER_ID    = "user1";
    private static final String SESSION_ID = "session1";

    private static final int IMAGE_MAX_DIM = 512;
    private static final int IMAGE_QUALITY = 70;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // -----------------------------------------------------------------------
    // Callback interfejsi
    // -----------------------------------------------------------------------

    public interface Callback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    public interface StreamCallback {
        void onAgentStart(String agentName);
        void onChunk(String agentName, String textChunk);
        void onComplete();
        void onFailure(String error);
    }

    // -----------------------------------------------------------------------
    // ask() — originalni, za kompatibilnost
    // -----------------------------------------------------------------------

    public static void ask(File imageFile, String question,
                           ColorBlindnessType type, Callback callback) {
        new Thread(() -> {
            try {
                ensureSession();
                String body = buildRequestBody(imageFile, question, type);
                Request request = new Request.Builder()
                        .url(BASE_URL + "/run")
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .post(RequestBody.create(body, MediaType.parse("application/json")))
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String rb = response.body().string();
                    if (!response.isSuccessful())
                        throw new Exception("Server error " + response.code());
                    callback.onSuccess(parseLastText(new JSONArray(rb)));
                }
            } catch (Exception e) {
                Log.e(TAG, "ask error: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }
        }).start();
    }

    // -----------------------------------------------------------------------
    // askStreaming() — SSE real-time stream via /run_sse
    // -----------------------------------------------------------------------

    public static void askStreaming(File imageFile, String question,
                                    ColorBlindnessType type, StreamCallback callback) {
        new Thread(() -> {
            try {
                ensureSession();
                String body = buildRequestBody(imageFile, question, type);

                Request request = new Request.Builder()
                        .url(BASE_URL + "/run_sse")
                        .addHeader("Accept", "text/event-stream")
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .post(RequestBody.create(body, MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        callback.onFailure("Server error " + response.code());
                        return;
                    }

                    BufferedSource source = response.body().source();
                    String currentAgent = null;

                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) continue;
                        if (!line.startsWith("data:")) continue;

                        String jsonStr = line.substring(5).trim();
                        if (jsonStr.isEmpty() || jsonStr.equals("[DONE]")) continue;

                        JSONObject event;
                        try {
                            event = new JSONObject(jsonStr);
                        } catch (JSONException e) {
                            continue;
                        }

                        String author      = event.optString("author", "");
                        JSONObject content = event.optJSONObject("content");
                        if (content == null) continue;
                        if (!"model".equals(content.optString("role", ""))) continue;

                        JSONArray parts = content.optJSONArray("parts");
                        if (parts == null) continue;

                        StringBuilder chunkText = new StringBuilder();
                        for (int i = 0; i < parts.length(); i++) {
                            String t = parts.getJSONObject(i).optString("text", "");
                            chunkText.append(t);
                        }
                        if (chunkText.length() == 0) continue;

                        if (!author.equals(currentAgent)) {
                            currentAgent = author;
                            callback.onAgentStart(author);
                        }

                        callback.onChunk(currentAgent, chunkText.toString());
                    }

                    callback.onComplete();
                }

            } catch (Exception e) {
                Log.e(TAG, "askStreaming error: " + e.getMessage());
                callback.onFailure(e.getMessage());
            }
        }).start();
    }

    // -----------------------------------------------------------------------
    // Zajednička logika
    // -----------------------------------------------------------------------

    private static void ensureSession() throws Exception {
        Request sessionReq = new Request.Builder()
                .url(BASE_URL + "/apps/" + APP_NAME + "/users/" + USER_ID + "/sessions/" + SESSION_ID)
                .addHeader("ngrok-skip-browser-warning", "true")
                .post(RequestBody.create("{}", MediaType.parse("application/json")))
                .build();
        client.newCall(sessionReq).execute().close();
    }

    private static String buildRequestBody(File imageFile, String question,
                                           ColorBlindnessType type) throws Exception {
        String base64Image = compressImageToBase64(imageFile);

        JSONObject inlineData = new JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", base64Image);
        JSONObject imagePart  = new JSONObject().put("inline_data", inlineData);
        JSONObject textPart   = new JSONObject().put("text", buildPrompt(question, type));

        JSONObject newMessage = new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(imagePart).put(textPart));

        return new JSONObject()
                .put("app_name", APP_NAME)
                .put("user_id", USER_ID)
                .put("session_id", SESSION_ID)
                .put("new_message", newMessage)
                .toString();
    }

    private static String compressImageToBase64(File imageFile) throws Exception {
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

        // EXIF rotacija
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        float angle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90)  angle = 90f;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) angle = 180f;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) angle = 270f;
        if (angle != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        // Skaliranje
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float scale = Math.min((float) IMAGE_MAX_DIM / w, (float) IMAGE_MAX_DIM / h);
        if (scale < 1f) {
            bitmap = Bitmap.createScaledBitmap(bitmap,
                    Math.round(w * scale), Math.round(h * scale), true);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String parseLastText(JSONArray events) {
        for (int i = events.length() - 1; i >= 0; i--) {
            try {
                JSONObject event = events.getJSONObject(i);
                if (!event.has("content")) continue;
                JSONArray parts = event.getJSONObject("content").optJSONArray("parts");
                if (parts == null) continue;
                for (int j = 0; j < parts.length(); j++) {
                    String text = parts.getJSONObject(j).optString("text", "");
                    if (!text.isEmpty()) return text;
                }
            } catch (JSONException ignored) {}
        }
        return "No response from agent.";
    }

    private static String buildPrompt(String userQuestion, ColorBlindnessType type) {
        String tl = typeLabel(type);
        String base = "The user has " + tl + ". Please analyse this image for someone with "
                + tl + ". Skip asking which type of colour blindness they have — it is already known.";
        if (userQuestion != null && !userQuestion.trim().isEmpty())
            return base + " Additional question from the user: " + userQuestion.trim();
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
}
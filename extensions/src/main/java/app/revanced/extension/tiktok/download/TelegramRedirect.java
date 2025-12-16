package app.revanced.extension.tiktok.download;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;

import app.revanced.extension.tiktok.settings.Settings;

/**
 * Handles redirection from TikTok download to Telegram bot.
 *
 * When user clicks download, instead of saving the video locally,
 * this opens your Telegram bot with the video information.
 */
public final class TelegramRedirect {
    private static final String TAG = "TelegramRedirect";

    // Store current video info when share menu opens
    private static String currentVideoId = null;
    private static String currentVideoUrl = null;
    private static String currentAuthor = null;

    /**
     * Check if Telegram redirect is enabled.
     */
    public static boolean isRedirectEnabled() {
        return Settings.isTelegramRedirectEnabled();
    }

    /**
     * Check if the clicked item is the download button.
     */
    public static boolean isDownloadItem(Object item) {
        if (item == null) {
            return false;
        }

        try {
            // Check various ways to identify download button
            String itemStr = item.toString().toLowerCase();
            if (itemStr.contains("download") || itemStr.contains("save")) {
                return true;
            }

            // Check type/id fields
            Object type = getFieldValue(item, "type");
            if (type != null && type.toString().toLowerCase().contains("download")) {
                return true;
            }

            Object id = getFieldValue(item, "id");
            if (id != null && id.toString().toLowerCase().contains("download")) {
                return true;
            }

            // Check for specific type IDs that TikTok uses for download
            // These IDs may vary by version
            Object typeId = getFieldValue(item, "typeId");
            if (typeId instanceof Integer) {
                int tid = (Integer) typeId;
                // Download button typically has specific type IDs (varies by version)
                // Common IDs: 0 for save video, 1 for save locally
                if (tid == 0 || tid == 1) {
                    return true;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking download item", e);
        }

        return false;
    }

    /**
     * Store information about the current video when share menu opens.
     */
    public static void storeCurrentVideoInfo(Object videoObject) {
        if (videoObject == null) {
            return;
        }

        try {
            // Try to extract video ID
            currentVideoId = extractStringField(videoObject, "awemeId", "id", "videoId", "aweme_id", "video_id");

            // Try to extract video URL
            currentVideoUrl = extractStringField(videoObject, "videoUrl", "playUrl", "downloadUrl",
                    "video_url", "play_url", "download_url");

            // Try to extract author username
            Object author = getFieldValue(videoObject, "author");
            if (author != null) {
                currentAuthor = extractStringField(author, "uniqueId", "username", "unique_id", "user_name");
            }

            Log.d(TAG, "Stored video info - ID: " + currentVideoId + ", Author: " + currentAuthor);

        } catch (Exception e) {
            Log.e(TAG, "Error storing video info", e);
        }
    }

    /**
     * Open Telegram bot (simple version without video info).
     */
    public static void openTelegramBot(Context context) {
        openTelegramBotWithVideo(context);
    }

    /**
     * Open Telegram bot with current video information.
     */
    public static void openTelegramBotWithVideo(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }

        try {
            String telegramUrl = buildTelegramUrl();

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Try Telegram app first
            intent.setPackage("org.telegram.messenger");

            try {
                context.startActivity(intent);
                showToast(context, "Opening Telegram...");
            } catch (Exception e) {
                // Fallback: try without package restriction (opens in browser or other Telegram client)
                intent.setPackage(null);
                try {
                    context.startActivity(intent);
                    showToast(context, "Opening Telegram...");
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to open Telegram", e2);
                    showToast(context, "Please install Telegram app");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening Telegram bot", e);
            showToast(context, "Error opening Telegram");
        }
    }

    /**
     * Redirect to Telegram with specific video URL.
     */
    public static void redirectToTelegram(Context context, String videoUrl) {
        if (videoUrl != null && !videoUrl.isEmpty()) {
            currentVideoUrl = videoUrl;
            // Try to extract video ID from URL
            if (currentVideoId == null) {
                currentVideoId = extractVideoIdFromUrl(videoUrl);
            }
        }
        openTelegramBotWithVideo(context);
    }

    /**
     * Build the Telegram URL based on settings.
     */
    private static String buildTelegramUrl() {
        String botUsername = Settings.getTelegramBotUsername();

        // Check for custom deep link format
        String customDeeplink = Settings.getCustomTelegramDeeplink();
        if (customDeeplink != null && !customDeeplink.isEmpty()) {
            return formatCustomDeeplink(customDeeplink);
        }

        // Build default t.me URL
        StringBuilder url = new StringBuilder("https://t.me/");
        url.append(botUsername);

        // Add video info as start parameter if enabled
        if (Settings.isIncludeVideoId() && currentVideoId != null && !currentVideoId.isEmpty()) {
            try {
                // Format: t.me/BotName?start=VIDEO_ID
                // Or with more info: t.me/BotName?start=VIDEO_ID_AUTHOR
                String startParam = currentVideoId;
                if (currentAuthor != null && !currentAuthor.isEmpty()) {
                    startParam = currentVideoId + "_" + currentAuthor;
                }
                url.append("?start=").append(URLEncoder.encode(startParam, "UTF-8"));
            } catch (Exception e) {
                url.append("?start=").append(currentVideoId);
            }
        }

        return url.toString();
    }

    /**
     * Format custom deep link with placeholders.
     */
    private static String formatCustomDeeplink(String template) {
        String result = template;

        if (currentVideoId != null) {
            result = result.replace("{VIDEO_ID}", currentVideoId);
        } else {
            result = result.replace("{VIDEO_ID}", "");
        }

        if (currentVideoUrl != null) {
            try {
                result = result.replace("{VIDEO_URL}", URLEncoder.encode(currentVideoUrl, "UTF-8"));
            } catch (Exception e) {
                result = result.replace("{VIDEO_URL}", currentVideoUrl);
            }
        } else {
            result = result.replace("{VIDEO_URL}", "");
        }

        if (currentAuthor != null) {
            result = result.replace("{AUTHOR}", currentAuthor);
        } else {
            result = result.replace("{AUTHOR}", "");
        }

        return result;
    }

    /**
     * Extract video ID from a TikTok URL.
     */
    private static String extractVideoIdFromUrl(String url) {
        if (url == null) {
            return null;
        }

        // Try to find video ID in URL patterns like:
        // /video/1234567890
        // ?item_id=1234567890
        // aweme_id=1234567890
        try {
            if (url.contains("/video/")) {
                int start = url.indexOf("/video/") + 7;
                int end = url.indexOf("?", start);
                if (end == -1) end = url.indexOf("/", start);
                if (end == -1) end = url.length();
                return url.substring(start, end);
            }

            if (url.contains("item_id=")) {
                int start = url.indexOf("item_id=") + 8;
                int end = url.indexOf("&", start);
                if (end == -1) end = url.length();
                return url.substring(start, end);
            }

            if (url.contains("aweme_id=")) {
                int start = url.indexOf("aweme_id=") + 9;
                int end = url.indexOf("&", start);
                if (end == -1) end = url.length();
                return url.substring(start, end);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error extracting video ID from URL", e);
        }

        return null;
    }

    /**
     * Extract a string field value trying multiple field names.
     */
    private static String extractStringField(Object obj, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object value = getFieldValue(obj, fieldName);
            if (value instanceof String && !((String) value).isEmpty()) {
                return (String) value;
            }

            // Try getter method
            value = invokeGetter(obj, fieldName);
            if (value instanceof String && !((String) value).isEmpty()) {
                return (String) value;
            }
        }
        return null;
    }

    /**
     * Get field value using reflection.
     */
    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (NoSuchFieldException ignored) {
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Invoke a getter method using reflection.
     */
    private static Object invokeGetter(Object obj, String fieldName) {
        try {
            String getterName = "get" + capitalize(fieldName);
            Method method = findMethod(obj.getClass(), getterName);
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(obj);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Find a method in class hierarchy.
     */
    private static Method findMethod(Class<?> clazz, String methodName) {
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 0) {
                    return method;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Capitalize first letter.
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Show a toast message.
     */
    private static void showToast(Context context, String message) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    /**
     * Clear stored video info.
     */
    public static void clearVideoInfo() {
        currentVideoId = null;
        currentVideoUrl = null;
        currentAuthor = null;
    }
}

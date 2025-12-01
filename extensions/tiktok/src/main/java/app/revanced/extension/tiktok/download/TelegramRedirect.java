package app.revanced.extension.tiktok.download;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import app.revanced.extension.tiktok.settings.Settings;

@SuppressWarnings("unused")
public class TelegramRedirect {

    private static String currentVideoId = null;
    private static String currentVideoUrl = null;

    /**
     * Check if Telegram redirect is enabled.
     */
    public static boolean isRedirectEnabled() {
        return Settings.TELEGRAM_REDIRECT_ENABLED.get();
    }

    /**
     * Store current video ID for later use.
     */
    public static void setCurrentVideoId(String videoId) {
        currentVideoId = videoId;
    }

    /**
     * Store current video URL for later use.
     */
    public static void setCurrentVideoUrl(String videoUrl) {
        currentVideoUrl = videoUrl;
        if (currentVideoId == null && videoUrl != null) {
            currentVideoId = extractVideoIdFromUrl(videoUrl);
        }
    }

    /**
     * Open Telegram bot with video info.
     */
    public static void openTelegramBot(Context context) {
        if (context == null) return;

        try {
            String botUsername = Settings.TELEGRAM_BOT_USERNAME.get();
            boolean includeVideoId = Settings.TELEGRAM_INCLUDE_VIDEO_ID.get();

            StringBuilder url = new StringBuilder("https://t.me/");
            url.append(botUsername);

            if (includeVideoId && currentVideoId != null && !currentVideoId.isEmpty()) {
                url.append("?start=").append(currentVideoId);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Try Telegram app first
            intent.setPackage("org.telegram.messenger");
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                // Fallback to any handler
                intent.setPackage(null);
                context.startActivity(intent);
            }

            Toast.makeText(context, "Opening Telegram...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(context, "Failed to open Telegram", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called when download button is clicked.
     * Returns true if handled (redirected to Telegram), false to proceed with normal download.
     */
    public static boolean onDownloadClick(Context context) {
        if (!isRedirectEnabled()) {
            return false;
        }

        openTelegramBot(context);
        return true;
    }

    /**
     * Extract video ID from TikTok URL.
     */
    private static String extractVideoIdFromUrl(String url) {
        if (url == null) return null;

        try {
            // Pattern: /video/1234567890
            if (url.contains("/video/")) {
                int start = url.indexOf("/video/") + 7;
                int end = url.indexOf("?", start);
                if (end == -1) end = url.indexOf("/", start);
                if (end == -1) end = Math.min(start + 20, url.length());
                return url.substring(start, end);
            }

            // Pattern: ?item_id=1234567890
            if (url.contains("item_id=")) {
                int start = url.indexOf("item_id=") + 8;
                int end = url.indexOf("&", start);
                if (end == -1) end = url.length();
                return url.substring(start, end);
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * Clear stored video info.
     */
    public static void clearVideoInfo() {
        currentVideoId = null;
        currentVideoUrl = null;
    }
}

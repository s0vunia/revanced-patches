package app.revanced.extension.tiktok.download;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.lang.reflect.Method;

import app.revanced.extension.tiktok.settings.Settings;

@SuppressWarnings("unused")
public class TelegramRedirect {

    private static String currentVideoId = null;
    private static String currentVideoUrl = null;
    private static String currentUsername = null;
    private static Application applicationContext = null;

    /**
     * Get application context using reflection.
     */
    private static Context getAppContext() {
        if (applicationContext != null) {
            return applicationContext;
        }
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getMethod("currentApplication");
            applicationContext = (Application) currentApplication.invoke(null);
            return applicationContext;
        } catch (Exception e) {
            return null;
        }
    }

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
     * Store current username for URL building.
     */
    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }

    /**
     * Build TikTok video URL from stored info.
     */
    private static String buildVideoUrl() {
        if (currentVideoUrl != null && !currentVideoUrl.isEmpty()) {
            return currentVideoUrl;
        }
        if (currentVideoId != null && !currentVideoId.isEmpty()) {
            // Build URL: https://www.tiktok.com/@username/video/VIDEO_ID
            // If no username, use generic format
            if (currentUsername != null && !currentUsername.isEmpty()) {
                return "https://www.tiktok.com/@" + currentUsername + "/video/" + currentVideoId;
            }
            // Fallback to vm.tiktok.com short link format
            return "https://vm.tiktok.com/" + currentVideoId;
        }
        return null;
    }

    /**
     * Copy video URL to clipboard.
     */
    private static void copyToClipboard(Context context, String text) {
        if (context == null || text == null) return;

        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("TikTok Video URL", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Ссылка скопирована", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Ignore clipboard errors
        }
    }

    /**
     * Open Telegram bot with video info.
     */
    public static void openTelegramBot(Context context) {
        if (context == null) {
            context = getAppContext();
        }
        if (context == null) return;

        try {
            String botUsername = Settings.TELEGRAM_BOT_USERNAME.get();
            boolean includeVideoId = Settings.TELEGRAM_INCLUDE_VIDEO_ID.get();

            // Copy video URL to clipboard before redirecting
            String videoUrl = buildVideoUrl();
            if (videoUrl != null) {
                copyToClipboard(context, videoUrl);
            }

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

        } catch (Exception e) {
            // Silently fail
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
        currentUsername = null;
    }

    /**
     * Called with Aweme object to extract and store video info.
     * Uses reflection to get fields from TikTok's Aweme class.
     */
    public static void setAwemeInfo(Object aweme) {
        if (aweme == null) return;

        try {
            // Try to get video ID (aid or awemeId field)
            String videoId = getStringField(aweme, "aid");
            if (videoId == null || videoId.isEmpty()) {
                videoId = getStringField(aweme, "awemeId");
            }
            if (videoId != null && !videoId.isEmpty()) {
                currentVideoId = videoId;
            }

            // Try to get share URL
            String shareUrl = getStringField(aweme, "shareUrl");
            if (shareUrl != null && !shareUrl.isEmpty()) {
                currentVideoUrl = shareUrl;
            }

            // Try to get author username
            Object author = getObjectField(aweme, "author");
            if (author != null) {
                String uniqueId = getStringField(author, "uniqueId");
                if (uniqueId != null && !uniqueId.isEmpty()) {
                    currentUsername = uniqueId;
                }
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    /**
     * Get String field from object using reflection.
     */
    private static String getStringField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                return value != null ? value.toString() : null;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Get Object field from object using reflection.
     */
    private static Object getObjectField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Find field in class hierarchy.
     */
    private static java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    // Track if this is a download action
    private static boolean isDownloadAction = false;

    /**
     * Called to capture aweme info from ACLCommonShare object.
     * ACLCommonShare contains reference to the current video being shared/downloaded.
     * Also checks if this is a download action (not just share menu).
     */
    public static void captureAwemeFromShare(Object aclCommonShare) {
        if (aclCommonShare == null) return;

        isDownloadAction = false;

        try {
            // Check if this is a download action by looking at share type
            // ACLCommonShare has different types: download, copy_link, share, etc.
            String shareType = getStringField(aclCommonShare, "shareType");
            if (shareType == null) {
                shareType = getStringField(aclCommonShare, "type");
            }
            if (shareType == null) {
                shareType = getStringField(aclCommonShare, "mShareType");
            }

            // Check for download-related indicators
            Integer showType = getIntField(aclCommonShare, "showType");
            Integer downloadType = getIntField(aclCommonShare, "downloadType");
            Boolean isDownload = getBooleanField(aclCommonShare, "isDownload");
            Boolean isSaveLocal = getBooleanField(aclCommonShare, "isSaveLocal");

            // Determine if this is a download action
            if (isDownload != null && isDownload) {
                isDownloadAction = true;
            } else if (isSaveLocal != null && isSaveLocal) {
                isDownloadAction = true;
            } else if (shareType != null && (
                    shareType.toLowerCase().contains("download") ||
                    shareType.toLowerCase().contains("save") ||
                    shareType.toLowerCase().contains("local"))) {
                isDownloadAction = true;
            } else if (downloadType != null && downloadType > 0) {
                isDownloadAction = true;
            }

            // If we can't determine, check class name for hints
            if (!isDownloadAction) {
                String className = aclCommonShare.getClass().getName().toLowerCase();
                if (className.contains("download") || className.contains("save")) {
                    isDownloadAction = true;
                }
            }

            // Only capture aweme info if this looks like a download
            if (!isDownloadAction) {
                return;
            }

            // Clear previous info
            clearVideoInfo();

            // Try to find aweme field in ACLCommonShare
            Object aweme = getObjectField(aclCommonShare, "aweme");
            if (aweme == null) {
                aweme = getObjectField(aclCommonShare, "mAweme");
            }
            if (aweme == null) {
                // Try to find any field of type Aweme
                for (java.lang.reflect.Field field : aclCommonShare.getClass().getDeclaredFields()) {
                    if (field.getType().getName().contains("Aweme")) {
                        field.setAccessible(true);
                        aweme = field.get(aclCommonShare);
                        if (aweme != null) break;
                    }
                }
            }

            if (aweme != null) {
                setAwemeInfo(aweme);
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    /**
     * Get Integer field from object using reflection.
     */
    private static Integer getIntField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Get Boolean field from object using reflection.
     */
    private static Boolean getBooleanField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Called early in download flow (from ACLCommonShare.getCode).
     * Returns -1 to block download if redirected, 0 to allow normal download.
     */
    public static int onDownloadCheck() {
        if (!isRedirectEnabled()) {
            return 0; // Allow normal action
        }

        // Only redirect if this is specifically a download action
        if (!isDownloadAction) {
            return 0; // Allow normal share menu/copy link/etc
        }

        openTelegramBot(null); // Will get context via reflection
        return -1; // Block download
    }
}

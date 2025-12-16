package app.revanced.extension.tiktok.download;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import app.revanced.extension.tiktok.settings.Settings;

/**
 * Extension to enhance TikTok download functionality.
 *
 * Provides:
 * - ACL bypass for download restrictions
 * - No-watermark URL retrieval
 * - Download URL processing
 */
public final class DownloadEnhancer {
    private static final String TAG = "TikTokDownload";

    // Current video object (stored when share menu opens)
    private static Object currentVideoObject = null;

    // Fields to search for no-watermark URLs
    private static final String[] NO_WATERMARK_URL_FIELDS = {
            "downloadAddr",
            "download_addr",
            "playAddrH265",
            "play_addr_h265",
            "playAddrBytevc1",
            "play_addr_bytevc1",
            "bitRateAddr",
            "bit_rate_addr"
    };

    /**
     * Check if ACL bypass is enabled.
     */
    public static boolean shouldBypassAcl() {
        return Settings.isDownloadEnabled();
    }

    /**
     * Store video object when share menu is opened.
     */
    public static void onShareMenuOpened(Object videoOrContext) {
        if (videoOrContext == null) {
            return;
        }

        try {
            // Try to extract video/aweme object
            Object video = extractVideoObject(videoOrContext);
            if (video != null) {
                currentVideoObject = video;
                Log.d(TAG, "Stored current video object: " + video.getClass().getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error storing video object", e);
        }
    }

    /**
     * Get no-watermark download URL for a video object.
     */
    public static String getNoWatermarkUrl(Object videoObject) {
        if (!Settings.isRemoveVideoWatermark() || videoObject == null) {
            return null;
        }

        try {
            // Try to find no-watermark URL in various fields
            for (String fieldName : NO_WATERMARK_URL_FIELDS) {
                String url = extractUrlFromField(videoObject, fieldName);
                if (url != null && !url.isEmpty()) {
                    Log.d(TAG, "Found no-watermark URL in field: " + fieldName);
                    return url;
                }
            }

            // Try to get from nested video object
            Object video = getFieldValue(videoObject, "video");
            if (video != null) {
                for (String fieldName : NO_WATERMARK_URL_FIELDS) {
                    String url = extractUrlFromField(video, fieldName);
                    if (url != null && !url.isEmpty()) {
                        return url;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting no-watermark URL", e);
        }

        return null;
    }

    /**
     * Process download URL to prefer no-watermark version.
     */
    public static String processDownloadUrl(String originalUrl) {
        if (!Settings.isRemoveVideoWatermark() || originalUrl == null) {
            return originalUrl;
        }

        // If we have a stored video object, try to get no-watermark URL
        if (currentVideoObject != null) {
            String noWatermarkUrl = getNoWatermarkUrl(currentVideoObject);
            if (noWatermarkUrl != null) {
                Log.d(TAG, "Using no-watermark URL instead of original");
                return noWatermarkUrl;
            }
        }

        // Modify URL parameters if possible
        String processedUrl = processUrlParameters(originalUrl);
        if (!processedUrl.equals(originalUrl)) {
            Log.d(TAG, "Processed URL parameters for no-watermark");
        }

        return processedUrl;
    }

    /**
     * Process URL parameters to request no-watermark version.
     */
    private static String processUrlParameters(String url) {
        if (url == null) {
            return null;
        }

        // Remove watermark parameters
        String processed = url;

        // Replace watermark=1 with watermark=0
        if (processed.contains("wm=1")) {
            processed = processed.replace("wm=1", "wm=0");
        }

        // Add no-watermark parameter if not present
        if (!processed.contains("wm=") && processed.contains("?")) {
            processed = processed + "&wm=0";
        } else if (!processed.contains("wm=")) {
            processed = processed + "?wm=0";
        }

        return processed;
    }

    /**
     * Extract URL string from a field that might be a UrlModel or String.
     */
    private static String extractUrlFromField(Object obj, String fieldName) {
        Object value = getFieldValue(obj, fieldName);
        if (value == null) {
            // Try getter method
            String methodName = "get" + capitalize(toCamelCase(fieldName));
            value = invokeMethod(obj, methodName);
        }

        if (value == null) {
            return null;
        }

        // If it's a String, return directly
        if (value instanceof String) {
            return (String) value;
        }

        // If it's a UrlModel-like object, try to get urlList
        return extractFirstUrl(value);
    }

    /**
     * Extract first URL from a UrlModel-like object.
     */
    private static String extractFirstUrl(Object urlModel) {
        if (urlModel == null) {
            return null;
        }

        try {
            // Try urlList field
            Object urlList = getFieldValue(urlModel, "urlList");
            if (urlList == null) {
                urlList = getFieldValue(urlModel, "url_list");
            }
            if (urlList == null) {
                urlList = invokeMethod(urlModel, "getUrlList");
            }

            if (urlList instanceof List) {
                List<?> list = (List<?>) urlList;
                if (!list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof String) {
                        return (String) first;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Extract video object from various container types.
     */
    private static Object extractVideoObject(Object container) {
        if (container == null) {
            return null;
        }

        String className = container.getClass().getName();

        // Check if it's already a video/aweme object
        if (className.contains("Aweme") || className.contains("Video")) {
            return container;
        }

        // Try common field names for video object
        String[] videoFields = {"aweme", "video", "item", "data", "model"};
        for (String fieldName : videoFields) {
            Object value = getFieldValue(container, fieldName);
            if (value != null) {
                String valueClass = value.getClass().getName();
                if (valueClass.contains("Aweme") || valueClass.contains("Video")) {
                    return value;
                }
            }
        }

        return null;
    }

    /**
     * Get field value via reflection.
     */
    private static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;

        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Invoke a no-arg method via reflection.
     */
    private static Object invokeMethod(Object obj, String methodName) {
        if (obj == null) return null;

        try {
            Method method = findMethod(obj.getClass(), methodName);
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(obj);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Find a field in a class hierarchy.
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Find a no-arg method in a class hierarchy.
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
     * Capitalize the first letter of a string.
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Convert snake_case to camelCase.
     */
    private static String toCamelCase(String str) {
        if (str == null || !str.contains("_")) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : str.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}

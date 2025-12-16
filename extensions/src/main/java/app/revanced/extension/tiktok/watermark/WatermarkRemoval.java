package app.revanced.extension.tiktok.watermark;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import app.revanced.extension.tiktok.settings.Settings;

/**
 * Extension for removing TikTok watermarks from videos and images.
 *
 * This class provides methods called by WatermarkRemovalPatch to:
 * - Check if watermark removal is enabled
 * - Get no-watermark video URLs
 * - Skip image watermark application
 */
public final class WatermarkRemoval {
    private static final String TAG = "TikTokWatermark";

    // URL patterns for no-watermark videos
    private static final String[] NO_WATERMARK_PATTERNS = {
            "play_addr_h265",
            "play_addr_bytevc1",
            "download_addr",
            "playwm",    // Contains "wm" but is usually watermarked
            "nowm",      // No watermark indicator
            "nwm"        // No watermark indicator
    };

    // Fields that might contain no-watermark URLs
    private static final String[] URL_FIELDS = {
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
     * Check if video watermark removal is enabled.
     */
    public static boolean shouldRemoveWatermark() {
        return Settings.isRemoveVideoWatermark();
    }

    /**
     * Check if image watermark removal is enabled.
     */
    public static boolean shouldRemoveImageWatermark() {
        return Settings.isRemovePicWatermark();
    }

    /**
     * Get no-watermark download address from video object.
     * Returns null if not found or watermark removal is disabled.
     */
    public static Object getNoWatermarkDownloadAddr(Object videoObject) {
        if (!Settings.isRemoveVideoWatermark() || videoObject == null) {
            return null;
        }

        try {
            // Try to find download_addr or alternative no-watermark URL
            for (String fieldName : URL_FIELDS) {
                Object urlStruct = getFieldValue(videoObject, fieldName);
                if (urlStruct != null && isValidUrlStruct(urlStruct)) {
                    Log.d(TAG, "Found no-watermark URL in field: " + fieldName);
                    return urlStruct;
                }
            }

            // Try getter methods
            for (String fieldName : URL_FIELDS) {
                String methodName = "get" + capitalize(toCamelCase(fieldName));
                Object urlStruct = invokeMethod(videoObject, methodName);
                if (urlStruct != null && isValidUrlStruct(urlStruct)) {
                    Log.d(TAG, "Found no-watermark URL via method: " + methodName);
                    return urlStruct;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting no-watermark download addr", e);
        }

        return null;
    }

    /**
     * Get no-watermark play address from video object.
     */
    public static Object getNoWatermarkPlayAddr(Object videoObject) {
        if (!Settings.isRemoveVideoWatermark() || videoObject == null) {
            return null;
        }

        try {
            // Prefer H265/ByteVC1 addresses as they're often watermark-free
            String[] preferredFields = {"playAddrH265", "play_addr_h265", "playAddrBytevc1", "play_addr_bytevc1"};

            for (String fieldName : preferredFields) {
                Object urlStruct = getFieldValue(videoObject, fieldName);
                if (urlStruct != null && isValidUrlStruct(urlStruct)) {
                    return urlStruct;
                }

                String methodName = "get" + capitalize(toCamelCase(fieldName));
                urlStruct = invokeMethod(videoObject, methodName);
                if (urlStruct != null && isValidUrlStruct(urlStruct)) {
                    return urlStruct;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting no-watermark play addr", e);
        }

        return null;
    }

    /**
     * Get clean video URI without watermark parameters.
     */
    public static String getCleanVideoUri(Object videoObject) {
        if (!Settings.isRemoveVideoWatermark() || videoObject == null) {
            return null;
        }

        try {
            // Try to get URL list from various fields
            String[] urlListFields = {"urlList", "url_list", "downloadAddr", "playAddr"};

            for (String fieldName : urlListFields) {
                Object value = getFieldValue(videoObject, fieldName);
                if (value == null) {
                    value = invokeMethod(videoObject, "get" + capitalize(toCamelCase(fieldName)));
                }

                if (value instanceof List) {
                    List<?> urlList = (List<?>) value;
                    String cleanUrl = findNoWatermarkUrl(urlList);
                    if (cleanUrl != null) {
                        return cleanUrl;
                    }
                } else if (value instanceof String) {
                    String url = (String) value;
                    if (isNoWatermarkUrl(url)) {
                        return url;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting clean video URI", e);
        }

        return null;
    }

    /**
     * Find a no-watermark URL from a list of URLs.
     */
    private static String findNoWatermarkUrl(List<?> urlList) {
        if (urlList == null || urlList.isEmpty()) {
            return null;
        }

        // First pass: look for explicit no-watermark URLs
        for (Object item : urlList) {
            if (item instanceof String) {
                String url = (String) item;
                if (url.contains("nowm") || url.contains("nwm") || url.contains("_nowatermark")) {
                    return url;
                }
            }
        }

        // Second pass: prefer URLs without watermark indicators
        for (Object item : urlList) {
            if (item instanceof String) {
                String url = (String) item;
                // Prefer URLs that don't explicitly contain watermark
                if (!url.contains("wm=1") && !url.contains("watermark=1")) {
                    return url;
                }
            }
        }

        // Fallback: return first URL
        Object first = urlList.get(0);
        return first instanceof String ? (String) first : null;
    }

    /**
     * Check if a URL is likely a no-watermark URL.
     */
    private static boolean isNoWatermarkUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("nowm") ||
                lowerUrl.contains("nwm") ||
                lowerUrl.contains("_nowatermark") ||
                (!lowerUrl.contains("wm=1") && !lowerUrl.contains("watermark=1"));
    }

    /**
     * Check if a URL struct object is valid (has URLs).
     */
    private static boolean isValidUrlStruct(Object urlStruct) {
        if (urlStruct == null) {
            return false;
        }

        try {
            // Try to get urlList field
            Object urlList = getFieldValue(urlStruct, "urlList");
            if (urlList == null) {
                urlList = getFieldValue(urlStruct, "url_list");
            }
            if (urlList == null) {
                urlList = invokeMethod(urlStruct, "getUrlList");
            }

            if (urlList instanceof List) {
                return !((List<?>) urlList).isEmpty();
            }
        } catch (Exception ignored) {
        }

        return false;
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

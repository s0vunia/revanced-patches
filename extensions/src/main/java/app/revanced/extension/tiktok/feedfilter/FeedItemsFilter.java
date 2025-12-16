package app.revanced.extension.tiktok.feedfilter;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import app.revanced.extension.tiktok.settings.Settings;

/**
 * Filter to remove ads and unwanted content from TikTok feed.
 *
 * This class is called by the FeedFilterPatch to filter feed items
 * before they are displayed to the user.
 */
public final class FeedItemsFilter {
    private static final String TAG = "TikTokAdFilter";

    // Ad-related field/method names to check
    private static final String[] AD_INDICATORS = {
            "isAd", "is_ad", "isPromoted", "is_promoted", "isSponsored", "is_sponsored",
            "adType", "ad_type", "promotedType", "promoted_type",
            "cellType", "cell_type", // Used for different content types
            "type" // Generic type field
    };

    // Ad type values
    private static final String[] AD_TYPE_VALUES = {
            "ad", "promoted", "sponsored", "commercial", "brand",
            "live", "story", "shop", "ecommerce"
    };

    // Package prefixes that indicate ad content
    private static final String[] AD_PACKAGE_PREFIXES = {
            "com.bytedance.sdk.openadsdk",
            "com.pangle",
            "com.ss.android.socialbase.ad",
            "com.ss.android.downloadad"
    };

    /**
     * Main entry point called by the patch.
     * Filters the feed item list object.
     */
    public static void filterFeedItems(Object feedItemList) {
        if (feedItemList == null || !Settings.isRemoveAds()) {
            return;
        }

        try {
            // Try to get the items list from the feed object
            List<?> items = extractItemsList(feedItemList);
            if (items != null) {
                filterList(items);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering feed items", e);
        }
    }

    /**
     * Filter a List directly.
     */
    public static List<?> filterFeedList(List<?> items) {
        if (items == null || !Settings.isRemoveAds()) {
            return items;
        }

        try {
            filterList(items);
        } catch (Exception e) {
            Log.e(TAG, "Error filtering feed list", e);
        }

        return items;
    }

    /**
     * Check if Pangle ad SDK should be blocked.
     */
    public static boolean shouldBlockAds() {
        return Settings.isBlockAdSdk();
    }

    /**
     * Check if an item should be marked as ad (for filtering).
     */
    public static boolean isAdItem(Object item) {
        if (item == null) {
            return false;
        }

        return shouldFilterItem(item);
    }

    /**
     * Extract the items list from a feed container object.
     */
    private static List<?> extractItemsList(Object container) {
        if (container instanceof List) {
            return (List<?>) container;
        }

        // Try common field names
        String[] listFieldNames = {"items", "itemList", "item_list", "awemeList", "aweme_list", "data", "list"};

        for (String fieldName : listFieldNames) {
            try {
                Field field = findField(container.getClass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(container);
                    if (value instanceof List) {
                        return (List<?>) value;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * Filter items from the list in-place.
     */
    private static void filterList(List<?> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int removedCount = 0;
        Iterator<?> iterator = items.iterator();

        while (iterator.hasNext()) {
            Object item = iterator.next();
            if (shouldFilterItem(item)) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            Log.d(TAG, "Filtered " + removedCount + " items from feed");
        }
    }

    /**
     * Determine if an item should be filtered out.
     */
    private static boolean shouldFilterItem(Object item) {
        if (item == null) {
            return false;
        }

        // Check class name for ad indicators
        String className = item.getClass().getName();
        for (String prefix : AD_PACKAGE_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }

        // Check for ad-related fields and methods
        Class<?> clazz = item.getClass();

        // Check boolean fields/methods
        for (String indicator : AD_INDICATORS) {
            // Try as method (isAd(), isPromoted(), etc.)
            Boolean result = invokeBooleanMethod(item, indicator);
            if (result != null && result) {
                return true;
            }

            result = invokeBooleanMethod(item, "get" + capitalize(indicator));
            if (result != null && result) {
                return true;
            }

            // Try as field
            Object fieldValue = getFieldValue(item, indicator);
            if (fieldValue instanceof Boolean && (Boolean) fieldValue) {
                return true;
            }
        }

        // Check type field for ad-related values
        Object typeValue = getFieldValue(item, "type");
        if (typeValue == null) {
            typeValue = getFieldValue(item, "cellType");
        }
        if (typeValue == null) {
            typeValue = getFieldValue(item, "itemType");
        }

        if (typeValue != null) {
            String typeStr = typeValue.toString().toLowerCase();

            // Check for ad types
            if (Settings.isRemoveAds() && (typeStr.contains("ad") || typeStr.contains("sponsored") || typeStr.contains("promoted"))) {
                return true;
            }

            // Check for live streams
            if (Settings.isRemoveLive() && typeStr.contains("live")) {
                return true;
            }

            // Check for stories
            if (Settings.isRemoveStories() && typeStr.contains("story")) {
                return true;
            }

            // Check for shop content
            if (Settings.isRemoveShop() && (typeStr.contains("shop") || typeStr.contains("ecommerce") || typeStr.contains("product"))) {
                return true;
            }
        }

        // Check for promoted/branded content through other indicators
        if (Settings.isRemovePromoted()) {
            Object promoted = getFieldValue(item, "isPromoted");
            if (promoted == null) promoted = getFieldValue(item, "promoted");
            if (promoted == null) promoted = getFieldValue(item, "isBrandedContent");
            if (promoted == null) promoted = getFieldValue(item, "brandedContent");

            if (promoted instanceof Boolean && (Boolean) promoted) {
                return true;
            }
            if (promoted instanceof Integer && (Integer) promoted > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Invoke a boolean method on an object.
     */
    private static Boolean invokeBooleanMethod(Object obj, String methodName) {
        try {
            Method method = findMethod(obj.getClass(), methodName);
            if (method != null && method.getReturnType() == boolean.class) {
                method.setAccessible(true);
                return (Boolean) method.invoke(obj);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Get the value of a field from an object.
     */
    private static Object getFieldValue(Object obj, String fieldName) {
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
     * Find a method in a class hierarchy.
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
}

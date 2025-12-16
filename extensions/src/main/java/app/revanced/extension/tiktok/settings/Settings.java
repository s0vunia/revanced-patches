package app.revanced.extension.tiktok.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Settings for TikTok ReVanced patches.
 *
 * Settings are stored in SharedPreferences and can be modified through:
 * 1. The in-app SettingsActivity
 * 2. The native library (libtikmod.so)
 *
 * SharedPreferences name: "tikmod_settings"
 * This matches what the native library uses for persistence.
 */
public final class Settings {

    private static final String PREFS_NAME = "tikmod_settings";
    private static SharedPreferences prefs;
    private static Context appContext;
    private static boolean nativeLoaded = false;

    /**
     * Native method to reload settings in native library.
     * Called after settings are changed in the UI.
     */
    private static native void nativeReloadSettings();

    static {
        try {
            System.loadLibrary("tikmod");
            nativeLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            // Native library not available, settings work in Java-only mode
            nativeLoaded = false;
        }
    }

    /**
     * Notify native library to reload settings from SharedPreferences.
     */
    public static void notifySettingsChanged() {
        if (nativeLoaded) {
            try {
                nativeReloadSettings();
            } catch (Exception e) {
                // Ignore native errors
            }
        }
    }

    /**
     * Initialize settings with application context.
     * Must be called early in app startup.
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getPrefs() {
        if (prefs == null && appContext != null) {
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        return prefs;
    }

    // ==================== TELEGRAM BOT SETTINGS ====================

    public static String getTelegramBotUsername() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getString("telegram_bot_username", "instagramavBot") : "instagramavBot";
    }

    public static void setTelegramBotUsername(String username) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putString("telegram_bot_username", username).apply();
    }

    public static boolean isIncludeVideoId() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("include_video_id", true) : true;
    }

    public static boolean isTelegramRedirectEnabled() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("download_via_telegram", false) : false;
    }

    public static String getCustomTelegramDeeplink() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getString("custom_telegram_deeplink", "") : "";
    }

    // ==================== AD FILTER SETTINGS ====================

    public static boolean isRemoveAds() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("hide_ads", true) : true;
    }

    public static boolean isRemovePromoted() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("hide_promotional_music", true) : true;
    }

    public static boolean isRemoveLive() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("hide_live_streams", false) : false;
    }

    public static boolean isRemoveStories() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remove_stories", false) : false;
    }

    public static boolean isRemoveShop() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remove_shop", true) : true;
    }

    public static boolean isBlockAdSdk() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("block_ad_sdk", true) : true;
    }

    // ==================== WATERMARK SETTINGS ====================

    public static boolean isRemoveVideoWatermark() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remove_video_watermark", true) : true;
    }

    public static boolean isRemovePicWatermark() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remove_pic_watermark", true) : true;
    }

    public static boolean isRemoveCommentPicWatermark() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remove_comment_pic_watermark", true) : true;
    }

    // ==================== DOWNLOAD SETTINGS ====================

    public static boolean isDownloadEnabled() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("enable_download", true) : true;
    }

    public static boolean isSingleImageModeDownload() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("single_image_mode_download", false) : false;
    }

    // ==================== FEED FILTER SETTINGS ====================

    public static boolean isHideImagePosts() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("hide_image_posts", false) : false;
    }

    public static boolean isHideLongPosts() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("hide_long_posts", false) : false;
    }

    public static boolean isFilterByViews() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("filter_by_views", false) : false;
    }

    public static long getMinViewsCount() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getLong("min_views_count", 0) : 0;
    }

    public static boolean isFilterByLikes() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("filter_by_likes", false) : false;
    }

    public static long getMinLikesCount() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getLong("min_likes_count", 0) : 0;
    }

    // ==================== UI SETTINGS ====================

    public static boolean isRememberPlaybackSpeed() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remember_playback_speed", true) : true;
    }

    public static float getPlaybackSpeed() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getFloat("playback_speed", 1.0f) : 1.0f;
    }

    public static void setPlaybackSpeed(float speed) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putFloat("playback_speed", speed).apply();
    }

    public static boolean isClearDisplayMode() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("clear_display_mode", false) : false;
    }

    // ==================== REGION SETTINGS ====================

    public static boolean isForceRegion() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("force_region", false) : false;
    }

    public static String getForcedRegion() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getString("forced_region", "") : "";
    }

    // ==================== DISCOVER ADS ====================

    public static boolean isRemoveSpotlightAds() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remove_spotlight_ads", true) : true;
    }

    public static boolean isRemoveDiscoverAds() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remove_discover_ads", true) : true;
    }

    // ==================== SDK BLOCKING ====================

    public static boolean isBlockAnalytics() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("block_analytics", true) : true;
    }

    // ==================== SSL PINNING BYPASS ====================

    /**
     * Check if SSL pinning bypass is enabled.
     * Default is false for security - user must explicitly enable.
     */
    public static boolean isSslBypassEnabled() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("ssl_bypass_enabled", false) : false;
    }

    public static void setSslBypassEnabled(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("ssl_bypass_enabled", enabled).apply();
    }

    // ==================== EMULATOR BYPASS ====================

    /**
     * Check if emulator bypass is enabled.
     * Default is false - user must explicitly enable for emulator use.
     */
    public static boolean isEmulatorBypassEnabled() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("emulator_bypass_enabled", false) : false;
    }

    public static void setEmulatorBypassEnabled(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("emulator_bypass_enabled", enabled).apply();
    }

    public static void setBlockAnalytics(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("block_analytics", enabled).apply();
    }

    public static void setBlockAdSdk(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("block_ad_sdk", enabled).apply();
    }

    public static void setRemoveStories(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("remove_stories", enabled).apply();
    }

    public static void setRemoveShop(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("remove_shop", enabled).apply();
    }

    // ==================== SETTERS FOR OTHER SETTINGS ====================

    public static void setIncludeVideoId(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("include_video_id", enabled).apply();
    }

    public static void setCustomTelegramDeeplink(String deeplink) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putString("custom_telegram_deeplink", deeplink).apply();
    }

    public static void setDownloadViaTelegram(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("download_via_telegram", enabled).apply();
    }

    public static void setHideAds(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("hide_ads", enabled).apply();
    }

    public static void setHideLiveStreams(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("hide_live_streams", enabled).apply();
    }

    public static void setHideImagePosts(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("hide_image_posts", enabled).apply();
    }

    public static void setHideLongPosts(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("hide_long_posts", enabled).apply();
    }

    /**
     * Open the settings activity.
     */
    public static void openSettings(Context context) {
        SettingsActivity.launch(context);
    }

    // ==================== REMOTE CONFIG SETTINGS ====================

    /**
     * Check if remote config is enabled.
     */
    public static boolean isRemoteConfigEnabled() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getBoolean("remote_config_enabled", false) : false;
    }

    public static void setRemoteConfigEnabled(boolean enabled) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putBoolean("remote_config_enabled", enabled).apply();
    }

    /**
     * Get the user-configured remote config URL.
     * Empty string means feature is disabled.
     */
    public static String getRemoteConfigUrl() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getString("remote_config_url", "") : "";
    }

    public static void setRemoteConfigUrl(String url) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putString("remote_config_url", url).apply();
    }

    /**
     * Get sync interval in hours (default: 6).
     */
    public static int getRemoteConfigSyncInterval() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getInt("remote_config_sync_interval", 6) : 6;
    }

    public static void setRemoteConfigSyncInterval(int hours) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putInt("remote_config_sync_interval", Math.max(1, hours)).apply();
    }

    /**
     * Get last sync time.
     */
    public static long getLastRemoteConfigSync() {
        SharedPreferences p = getPrefs();
        return p != null ? p.getLong("remote_config_last_sync", 0) : 0;
    }

    public static void setLastRemoteConfigSync(long timestamp) {
        SharedPreferences p = getPrefs();
        if (p != null) p.edit().putLong("remote_config_last_sync", timestamp).apply();
    }
}

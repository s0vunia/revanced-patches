package app.revanced.extension.tiktok.remoteconfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Remote Config data model.
 *
 * This class represents the configuration fetched from the user's remote config URL.
 * Users can host this config on their own GitHub Pages or any static hosting service.
 *
 * Example config.json:
 * {
 *   "configVersion": 1,
 *   "latestModVersion": "1.0.0",
 *   "updateUrl": "https://github.com/YOUR_REPO/releases",
 *   "updateMessage": "New version available with bug fixes",
 *   "features": {
 *     "removeWatermark": true,
 *     "removeAds": true,
 *     "enableDownload": true
 *   },
 *   "filterPresets": [...],
 *   "announcement": {
 *     "enabled": true,
 *     "message": "Welcome!",
 *     "url": "https://example.com"
 *   }
 * }
 */
public class RemoteConfig {

    // Config metadata
    public int configVersion;
    public String latestModVersion;
    public String updateUrl;
    public String updateMessage;

    // Feature toggles (suggestions only - local settings take precedence)
    public FeatureToggles features;

    // Filter presets
    public List<FilterPreset> filterPresets;

    // Announcement
    public Announcement announcement;

    // Cache metadata
    public long fetchedAt;

    /**
     * Parse remote config from JSON string.
     */
    public static RemoteConfig fromJson(String jsonString) throws Exception {
        JSONObject json = new JSONObject(jsonString);
        RemoteConfig config = new RemoteConfig();

        config.configVersion = json.optInt("configVersion", 1);
        config.latestModVersion = json.optString("latestModVersion", "");
        config.updateUrl = json.optString("updateUrl", "");
        config.updateMessage = json.optString("updateMessage", "");

        // Parse features
        if (json.has("features")) {
            config.features = FeatureToggles.fromJson(json.getJSONObject("features"));
        } else {
            config.features = new FeatureToggles();
        }

        // Parse filter presets
        config.filterPresets = new ArrayList<>();
        if (json.has("filterPresets")) {
            JSONArray presetsArray = json.getJSONArray("filterPresets");
            for (int i = 0; i < presetsArray.length(); i++) {
                config.filterPresets.add(FilterPreset.fromJson(presetsArray.getJSONObject(i)));
            }
        }

        // Parse announcement
        if (json.has("announcement")) {
            config.announcement = Announcement.fromJson(json.getJSONObject("announcement"));
        }

        config.fetchedAt = System.currentTimeMillis();
        return config;
    }

    /**
     * Check if config is still valid (not expired).
     */
    public boolean isValid(long maxAgeMillis) {
        return (System.currentTimeMillis() - fetchedAt) < maxAgeMillis;
    }

    /**
     * Check if an update is available.
     */
    public boolean hasUpdate(String currentVersion) {
        if (latestModVersion == null || latestModVersion.isEmpty()) {
            return false;
        }
        return compareVersions(latestModVersion, currentVersion) > 0;
    }

    /**
     * Compare two version strings (e.g., "1.0.0" vs "1.1.0").
     */
    private static int compareVersions(String v1, String v2) {
        if (v1 == null || v2 == null) return 0;

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

    private static int parseVersionPart(String part) {
        try {
            // Remove any non-numeric suffix (e.g., "1-beta" -> "1")
            return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Feature toggles from remote config.
     * These are suggestions only - local settings always take precedence.
     */
    public static class FeatureToggles {
        public boolean removeWatermark = true;
        public boolean removeAds = true;
        public boolean enableDownload = true;
        public boolean removePromoted = true;
        public boolean removeLive = false;
        public boolean removeStories = false;
        public boolean removeShop = true;
        public boolean blockAdSdk = true;
        public boolean sslBypass = false;
        public boolean emulatorBypass = false;

        public static FeatureToggles fromJson(JSONObject json) {
            FeatureToggles toggles = new FeatureToggles();
            toggles.removeWatermark = json.optBoolean("removeWatermark", true);
            toggles.removeAds = json.optBoolean("removeAds", true);
            toggles.enableDownload = json.optBoolean("enableDownload", true);
            toggles.removePromoted = json.optBoolean("removePromoted", true);
            toggles.removeLive = json.optBoolean("removeLive", false);
            toggles.removeStories = json.optBoolean("removeStories", false);
            toggles.removeShop = json.optBoolean("removeShop", true);
            toggles.blockAdSdk = json.optBoolean("blockAdSdk", true);
            toggles.sslBypass = json.optBoolean("sslBypass", false);
            toggles.emulatorBypass = json.optBoolean("emulatorBypass", false);
            return toggles;
        }
    }

    /**
     * Filter preset from remote config.
     */
    public static class FilterPreset {
        public String name;
        public String description;
        public long minViews;
        public long minLikes;
        public boolean hideImages;
        public boolean hideLongVideos;

        public static FilterPreset fromJson(JSONObject json) {
            FilterPreset preset = new FilterPreset();
            preset.name = json.optString("name", "");
            preset.description = json.optString("description", "");
            preset.minViews = json.optLong("minViews", 0);
            preset.minLikes = json.optLong("minLikes", 0);
            preset.hideImages = json.optBoolean("hideImages", false);
            preset.hideLongVideos = json.optBoolean("hideLongVideos", false);
            return preset;
        }
    }

    /**
     * Announcement from remote config.
     */
    public static class Announcement {
        public boolean enabled;
        public String title;
        public String message;
        public String url;
        public String dismissKey; // Used to track if user dismissed this announcement

        public static Announcement fromJson(JSONObject json) {
            Announcement announcement = new Announcement();
            announcement.enabled = json.optBoolean("enabled", false);
            announcement.title = json.optString("title", "");
            announcement.message = json.optString("message", "");
            announcement.url = json.optString("url", "");
            announcement.dismissKey = json.optString("dismissKey", "");
            return announcement;
        }
    }
}

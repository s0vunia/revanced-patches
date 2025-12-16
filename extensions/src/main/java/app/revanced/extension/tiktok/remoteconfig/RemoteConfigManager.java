package app.revanced.extension.tiktok.remoteconfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Remote Config Manager.
 *
 * Handles fetching, caching, and managing remote configuration.
 *
 * Security principles:
 * 1. URL is user-configurable (not hardcoded)
 * 2. Local settings always take precedence over remote
 * 3. No kill switch - config cannot disable the app
 * 4. Additive only - remote can only suggest defaults
 * 5. Offline fallback - app works fully without network
 * 6. Disabled by default - only works if user sets a URL
 */
public final class RemoteConfigManager {
    private static final String TAG = "TikTokRemoteConfig";

    private static final String PREFS_NAME = "tikmod_remoteconfig";
    private static final String KEY_CONFIG_URL = "config_url";
    private static final String KEY_CACHED_CONFIG = "cached_config";
    private static final String KEY_CACHE_TIME = "cache_time";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_SYNC_INTERVAL = "sync_interval_hours";
    private static final String KEY_DISMISSED_ANNOUNCEMENTS = "dismissed_announcements";

    // Default sync interval: 6 hours
    private static final int DEFAULT_SYNC_INTERVAL_HOURS = 6;
    private static final long HOUR_IN_MILLIS = 60 * 60 * 1000;

    private static Context appContext;
    private static RemoteConfig cachedConfig;
    private static ExecutorService executor;
    private static Handler mainHandler;

    // Callbacks
    public interface ConfigCallback {
        void onConfigLoaded(RemoteConfig config);
        void onError(String error);
    }

    public interface UpdateCheckCallback {
        void onUpdateAvailable(String version, String url, String message);
        void onNoUpdate();
        void onError(String error);
    }

    /**
     * Initialize the manager with application context.
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Try to load cached config
        loadCachedConfig();

        // Auto-sync if enabled and URL is configured
        if (isEnabled() && !getConfigUrl().isEmpty()) {
            syncIfNeeded(null);
        }
    }

    /**
     * Check if remote config is enabled.
     */
    public static boolean isEnabled() {
        SharedPreferences prefs = getPrefs();
        return prefs != null && prefs.getBoolean(KEY_ENABLED, false);
    }

    /**
     * Set whether remote config is enabled.
     */
    public static void setEnabled(boolean enabled) {
        SharedPreferences prefs = getPrefs();
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        }
    }

    /**
     * Get the user-configured remote config URL.
     */
    public static String getConfigUrl() {
        SharedPreferences prefs = getPrefs();
        return prefs != null ? prefs.getString(KEY_CONFIG_URL, "") : "";
    }

    /**
     * Set the remote config URL.
     */
    public static void setConfigUrl(String url) {
        SharedPreferences prefs = getPrefs();
        if (prefs != null) {
            prefs.edit().putString(KEY_CONFIG_URL, url).apply();
            // Clear cache when URL changes
            cachedConfig = null;
            prefs.edit().remove(KEY_CACHED_CONFIG).remove(KEY_CACHE_TIME).apply();
        }
    }

    /**
     * Get sync interval in hours.
     */
    public static int getSyncIntervalHours() {
        SharedPreferences prefs = getPrefs();
        return prefs != null ? prefs.getInt(KEY_SYNC_INTERVAL, DEFAULT_SYNC_INTERVAL_HOURS) : DEFAULT_SYNC_INTERVAL_HOURS;
    }

    /**
     * Set sync interval in hours.
     */
    public static void setSyncIntervalHours(int hours) {
        SharedPreferences prefs = getPrefs();
        if (prefs != null) {
            prefs.edit().putInt(KEY_SYNC_INTERVAL, Math.max(1, hours)).apply();
        }
    }

    /**
     * Get cached config if available.
     */
    public static RemoteConfig getCachedConfig() {
        return cachedConfig;
    }

    /**
     * Sync config if cache has expired.
     */
    public static void syncIfNeeded(ConfigCallback callback) {
        if (!isEnabled() || getConfigUrl().isEmpty()) {
            if (callback != null) {
                callback.onError("Remote config not enabled or URL not set");
            }
            return;
        }

        long cacheMaxAge = getSyncIntervalHours() * HOUR_IN_MILLIS;
        if (cachedConfig != null && cachedConfig.isValid(cacheMaxAge)) {
            if (callback != null) {
                callback.onConfigLoaded(cachedConfig);
            }
            return;
        }

        // Cache expired or not available, fetch new config
        fetchConfig(callback);
    }

    /**
     * Force sync config regardless of cache.
     */
    public static void forceSync(ConfigCallback callback) {
        if (!isEnabled() || getConfigUrl().isEmpty()) {
            if (callback != null) {
                callback.onError("Remote config not enabled or URL not set");
            }
            return;
        }

        fetchConfig(callback);
    }

    /**
     * Check for updates asynchronously.
     */
    public static void checkForUpdates(String currentVersion, UpdateCheckCallback callback) {
        if (cachedConfig != null && cachedConfig.hasUpdate(currentVersion)) {
            if (callback != null) {
                callback.onUpdateAvailable(
                        cachedConfig.latestModVersion,
                        cachedConfig.updateUrl,
                        cachedConfig.updateMessage
                );
            }
            return;
        }

        // Fetch fresh config and check
        forceSync(new ConfigCallback() {
            @Override
            public void onConfigLoaded(RemoteConfig config) {
                if (callback != null) {
                    if (config.hasUpdate(currentVersion)) {
                        callback.onUpdateAvailable(
                                config.latestModVersion,
                                config.updateUrl,
                                config.updateMessage
                        );
                    } else {
                        callback.onNoUpdate();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Check if an announcement has been dismissed.
     */
    public static boolean isAnnouncementDismissed(String dismissKey) {
        if (dismissKey == null || dismissKey.isEmpty()) return false;

        SharedPreferences prefs = getPrefs();
        String dismissed = prefs != null ? prefs.getString(KEY_DISMISSED_ANNOUNCEMENTS, "") : "";
        return dismissed.contains(dismissKey);
    }

    /**
     * Mark an announcement as dismissed.
     */
    public static void dismissAnnouncement(String dismissKey) {
        if (dismissKey == null || dismissKey.isEmpty()) return;

        SharedPreferences prefs = getPrefs();
        if (prefs != null) {
            String dismissed = prefs.getString(KEY_DISMISSED_ANNOUNCEMENTS, "");
            if (!dismissed.contains(dismissKey)) {
                dismissed = dismissed.isEmpty() ? dismissKey : dismissed + "," + dismissKey;
                prefs.edit().putString(KEY_DISMISSED_ANNOUNCEMENTS, dismissed).apply();
            }
        }
    }

    /**
     * Fetch config from remote URL.
     */
    private static void fetchConfig(ConfigCallback callback) {
        String url = getConfigUrl();
        if (url.isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Config URL not set"));
            }
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "Fetching remote config from: " + url);

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP error: " + responseCode);
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                String jsonString = response.toString();
                RemoteConfig config = RemoteConfig.fromJson(jsonString);

                // Cache the config
                cacheConfig(jsonString);
                cachedConfig = config;

                Log.d(TAG, "Remote config fetched successfully, version: " + config.configVersion);

                if (callback != null) {
                    mainHandler.post(() -> callback.onConfigLoaded(config));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching remote config", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Load cached config from SharedPreferences.
     */
    private static void loadCachedConfig() {
        SharedPreferences prefs = getPrefs();
        if (prefs == null) return;

        String cachedJson = prefs.getString(KEY_CACHED_CONFIG, "");
        if (cachedJson.isEmpty()) return;

        try {
            cachedConfig = RemoteConfig.fromJson(cachedJson);
            cachedConfig.fetchedAt = prefs.getLong(KEY_CACHE_TIME, 0);
            Log.d(TAG, "Loaded cached config");
        } catch (Exception e) {
            Log.e(TAG, "Error loading cached config", e);
            cachedConfig = null;
        }
    }

    /**
     * Cache config to SharedPreferences.
     */
    private static void cacheConfig(String jsonString) {
        SharedPreferences prefs = getPrefs();
        if (prefs != null) {
            prefs.edit()
                    .putString(KEY_CACHED_CONFIG, jsonString)
                    .putLong(KEY_CACHE_TIME, System.currentTimeMillis())
                    .apply();
        }
    }

    private static SharedPreferences getPrefs() {
        if (appContext == null) return null;
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}

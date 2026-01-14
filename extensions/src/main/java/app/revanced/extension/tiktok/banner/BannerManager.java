package app.revanced.extension.tiktok.banner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import app.revanced.extension.tiktok.remoteconfig.RemoteConfig;
import app.revanced.extension.tiktok.remoteconfig.RemoteConfigManager;

/**
 * Banner Manager for displaying custom announcements.
 *
 * Shows banners based on remote config when:
 * 1. App starts (MainActivity.onCreate)
 * 2. Remote config is enabled and has a config URL set
 * 3. Announcement is enabled in the config
 * 4. User hasn't dismissed this specific announcement (tracked by dismissKey)
 *
 * Banner will NOT show if:
 * - Remote config is disabled
 * - No config URL is set
 * - Announcement.enabled = false
 * - User already dismissed this announcement
 * - Network fetch failed and no cached config exists
 */
public final class BannerManager {
    private static final String TAG = "TikTokBanner";

    // Delay before showing banner (let app UI settle first)
    private static final long SHOW_DELAY_MS = 1500;

    // Prevent multiple banners from showing
    private static boolean bannerShown = false;

    /**
     * Called from MainActivity.onCreate() via bytecode patch.
     * Shows banner if conditions are met.
     */
    public static void onMainActivityCreated(Activity activity) {
        if (activity == null) {
            Log.d(TAG, "Activity is null, skipping banner");
            return;
        }

        if (bannerShown) {
            Log.d(TAG, "Banner already shown this session, skipping");
            return;
        }

        // Initialize RemoteConfigManager if not already done
        RemoteConfigManager.init(activity);

        // Check if remote config is enabled
        if (!RemoteConfigManager.isEnabled()) {
            Log.d(TAG, "Remote config disabled, skipping banner");
            return;
        }

        // Delay showing banner to let app UI settle
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showBannerIfNeeded(activity);
        }, SHOW_DELAY_MS);
    }

    /**
     * Check conditions and show banner if appropriate.
     */
    private static void showBannerIfNeeded(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.d(TAG, "Activity not available, skipping banner");
            return;
        }

        // Try to get cached config first
        RemoteConfig config = RemoteConfigManager.getCachedConfig();

        if (config != null) {
            tryShowBanner(activity, config);
        } else {
            // No cached config, try to fetch
            Log.d(TAG, "No cached config, fetching...");
            RemoteConfigManager.syncIfNeeded(new RemoteConfigManager.ConfigCallback() {
                @Override
                public void onConfigLoaded(RemoteConfig config) {
                    tryShowBanner(activity, config);
                }

                @Override
                public void onError(String error) {
                    Log.d(TAG, "Failed to fetch config: " + error);
                }
            });
        }
    }

    /**
     * Try to show banner from config.
     */
    private static void tryShowBanner(Activity activity, RemoteConfig config) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (config == null || config.announcement == null) {
            Log.d(TAG, "No announcement in config");
            return;
        }

        RemoteConfig.Announcement announcement = config.announcement;

        // Check if announcement is enabled
        if (!announcement.enabled) {
            Log.d(TAG, "Announcement disabled in config");
            return;
        }

        // Check if already dismissed
        String dismissKey = announcement.dismissKey;
        if (dismissKey != null && !dismissKey.isEmpty()) {
            if (RemoteConfigManager.isAnnouncementDismissed(dismissKey)) {
                Log.d(TAG, "Announcement already dismissed: " + dismissKey);
                return;
            }
        }

        // Show the banner!
        bannerShown = true;
        showBannerDialog(activity, announcement);
    }

    /**
     * Display the banner as an AlertDialog.
     */
    private static void showBannerDialog(Activity activity, RemoteConfig.Announcement announcement) {
        try {
            String title = announcement.title;
            String message = announcement.message;
            String url = announcement.url;
            String dismissKey = announcement.dismissKey;

            // Use defaults if empty
            if (title == null || title.isEmpty()) {
                title = "Announcement";
            }
            if (message == null || message.isEmpty()) {
                Log.d(TAG, "Empty announcement message, skipping");
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Mark as dismissed
                    if (dismissKey != null && !dismissKey.isEmpty()) {
                        RemoteConfigManager.dismissAnnouncement(dismissKey);
                    }
                    dialog.dismiss();
                });

            // Add "Learn More" button if URL is provided
            if (url != null && !url.isEmpty()) {
                builder.setNeutralButton("Learn More", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to open URL: " + url, e);
                    }
                    // Don't dismiss announcement when clicking Learn More
                });
            }

            // Add "Don't show again" button
            builder.setNegativeButton("Don't show again", (dialog, which) -> {
                if (dismissKey != null && !dismissKey.isEmpty()) {
                    RemoteConfigManager.dismissAnnouncement(dismissKey);
                }
                dialog.dismiss();
            });

            AlertDialog dialog = builder.create();
            dialog.show();

            Log.d(TAG, "Banner shown: " + title);

        } catch (Exception e) {
            Log.e(TAG, "Failed to show banner dialog", e);
        }
    }

    /**
     * Reset banner state (for testing).
     */
    public static void resetBannerState() {
        bannerShown = false;
    }
}

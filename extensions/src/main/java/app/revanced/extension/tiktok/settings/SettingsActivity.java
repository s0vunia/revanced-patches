package app.revanced.extension.tiktok.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import app.revanced.extension.tiktok.remoteconfig.RemoteConfig;
import app.revanced.extension.tiktok.remoteconfig.RemoteConfigManager;

/**
 * Settings Activity for TikTok ReVanced.
 * Provides in-app configuration for all mod features.
 */
public class SettingsActivity extends Activity {

    private static final String PREFS_NAME = "tikmod_settings";
    private SharedPreferences prefs;
    private LinearLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Create scrollable layout
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Header
        addHeader("TikTok ReVanced Settings");
        addDivider();

        // Watermark Section
        addSectionHeader("Watermark Removal");
        addSwitch("remove_video_watermark", "Remove Video Watermark",
            "Remove TikTok watermark from downloaded videos", true);
        addSwitch("remove_pic_watermark", "Remove Image Watermark",
            "Remove watermark from downloaded images", true);
        addSwitch("remove_comment_pic_watermark", "Remove Comment Image Watermark",
            "Remove watermark from comment images", true);
        addDivider();

        // Download Section
        addSectionHeader("Download Settings");
        addSwitch("enable_download", "Enable Download",
            "Enable video/image download feature", true);
        addSwitch("download_via_telegram", "Download via Telegram",
            "Redirect downloads to Telegram bot", false);
        addSwitch("single_image_mode_download", "Single Image Mode",
            "Download images individually instead of as slideshow", false);
        addDivider();

        // Telegram Bot Section
        addSectionHeader("Telegram Bot");
        addTextInput("telegram_bot_username", "Bot Username", "instagramavBot");
        addSwitch("include_video_id", "Include Video ID",
            "Include video ID in Telegram message", true);
        addTextInput("custom_telegram_deeplink", "Custom Deeplink", "");
        addDivider();

        // Block SDK Section
        addSectionHeader("SDK Blocking");
        addSwitch("block_ad_sdk", "Block Ad SDK",
            "Block Pangle/Bytedance ad SDK initialization", true);
        addSwitch("block_analytics", "Block Analytics",
            "Block analytics and tracking SDK", true);
        addSwitch("remove_stories", "Remove Stories",
            "Hide stories from feed", false);
        addSwitch("remove_shop", "Remove Shop Tab",
            "Hide TikTok Shop features", true);
        addDivider();

        // SSL/Debug Section
        addSectionHeader("Debug (Advanced)");
        addSwitch("ssl_bypass_enabled", "Disable SSL Pinning",
            "Allow HTTPS inspection with proxy tools (Burp/mitmproxy). WARNING: Security risk!", false);
        addSwitch("emulator_bypass_enabled", "Bypass Emulator Detection",
            "Allow TikTok to run on emulators by spoofing device properties", false);
        addDivider();

        // Feed Filter Section
        addSectionHeader("Feed Filtering");
        addSwitch("hide_ads", "Hide Ads",
            "Remove all advertisements from feed", true);
        addSwitch("hide_live_streams", "Hide Live Streams",
            "Filter out live streams from feed", false);
        addSwitch("hide_image_posts", "Hide Image Posts",
            "Filter out image/slideshow posts from feed", false);
        addSwitch("hide_long_posts", "Hide Long Videos",
            "Filter out videos longer than 3 minutes", false);
        addSwitch("hide_promotional_music", "Hide Promotional Music",
            "Filter posts with promotional/sponsored music", true);
        addDivider();

        // View/Like Filters
        addSectionHeader("Engagement Filters");
        addSwitch("filter_by_views", "Filter by Views",
            "Only show posts with minimum view count", false);
        addNumberInput("min_views_count", "Minimum Views", 0);
        addSwitch("filter_by_likes", "Filter by Likes",
            "Only show posts with minimum like count", false);
        addNumberInput("min_likes_count", "Minimum Likes", 0);
        addDivider();

        // UI Settings
        addSectionHeader("UI Settings");
        addSwitch("remember_playback_speed", "Remember Playback Speed",
            "Remember last used playback speed", true);
        addSwitch("clear_display_mode", "Clear Display Mode",
            "Hide UI elements during video playback", false);
        addDivider();

        // Ad Removal
        addSectionHeader("Ad Removal");
        addSwitch("remove_spotlight_ads", "Remove Spotlight Ads",
            "Remove ads from spotlight/explore", true);
        addSwitch("remove_discover_ads", "Remove Discover Ads",
            "Remove banner ads from discover page", true);
        addDivider();

        // Region Settings
        addSectionHeader("Region Settings");
        addSwitch("force_region", "Force Region",
            "Override detected region", false);
        addTextInput("forced_region", "Region Code", "US");
        addDivider();

        // Remote Config Section
        addSectionHeader("Remote Config");
        addSwitch("remote_config_enabled", "Enable Remote Config",
            "Sync settings from your own config URL (optional)", false);
        addTextInput("remote_config_url", "Config URL", "");
        addInfoText("Enter your GitHub Pages or server URL (e.g., https://yourname.github.io/config.json)");
        addNumberInputInt("remote_config_sync_interval", "Sync Interval (hours)", 6);
        addButton("Sync Now", this::syncRemoteConfig);
        addButton("Check for Updates", this::checkForUpdates);

        // About Section
        addDivider();
        addSectionHeader("About");
        addInfoText("TikTok ReVanced v1.0");
        addInfoText("Local-only settings - remote config is optional");

        scrollView.addView(mainLayout);
        setContentView(scrollView);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value,
            getResources().getDisplayMetrics()
        );
    }

    private void addHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        header.setTextColor(Color.WHITE);
        header.setPadding(0, dp(8), 0, dp(16));
        mainLayout.addView(header);
    }

    private void addSectionHeader(String text) {
        TextView section = new TextView(this);
        section.setText(text);
        section.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        section.setTextColor(Color.parseColor("#FE2C55")); // TikTok pink
        section.setPadding(0, dp(16), 0, dp(8));
        mainLayout.addView(section);
    }

    private void addSwitch(String key, String title, String description, boolean defaultValue) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(0, dp(8), 0, dp(8));

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setTextColor(Color.WHITE);
        textContainer.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        descView.setTextColor(Color.GRAY);
        textContainer.addView(descView);

        container.addView(textContainer);

        Switch switchView = new Switch(this);
        switchView.setChecked(prefs.getBoolean(key, defaultValue));
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(key, isChecked).apply();
            Settings.notifySettingsChanged();
        });
        container.addView(switchView);

        mainLayout.addView(container);
    }

    private void addNumberInput(String key, String title, long defaultValue) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(0, dp(4), 0, dp(8));

        TextView label = new TextView(this);
        label.setText(title + ": ");
        label.setTextColor(Color.WHITE);
        label.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        container.addView(label);

        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(prefs.getLong(key, defaultValue)));
        input.setTextColor(Color.WHITE);
        input.setMinWidth(dp(100));
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    long value = Long.parseLong(input.getText().toString());
                    prefs.edit().putLong(key, value).apply();
                } catch (NumberFormatException e) {
                    input.setText("0");
                    prefs.edit().putLong(key, 0).apply();
                }
                Settings.notifySettingsChanged();
            }
        });
        container.addView(input);

        mainLayout.addView(container);
    }

    private void addTextInput(String key, String title, String defaultValue) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(0, dp(4), 0, dp(8));

        TextView label = new TextView(this);
        label.setText(title + ": ");
        label.setTextColor(Color.WHITE);
        label.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        container.addView(label);

        EditText input = new EditText(this);
        input.setText(prefs.getString(key, defaultValue));
        input.setTextColor(Color.WHITE);
        input.setMinWidth(dp(100));
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                prefs.edit().putString(key, input.getText().toString()).apply();
                Settings.notifySettingsChanged();
            }
        });
        container.addView(input);

        mainLayout.addView(container);
    }

    private void addDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        );
        params.setMargins(0, dp(8), 0, dp(8));
        divider.setLayoutParams(params);
        mainLayout.addView(divider);
    }

    private void addInfoText(String text) {
        TextView info = new TextView(this);
        info.setText(text);
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        info.setTextColor(Color.GRAY);
        info.setPadding(0, dp(4), 0, dp(4));
        mainLayout.addView(info);
    }

    private void addNumberInputInt(String key, String title, int defaultValue) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(0, dp(4), 0, dp(8));

        TextView label = new TextView(this);
        label.setText(title + ": ");
        label.setTextColor(Color.WHITE);
        label.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        container.addView(label);

        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(prefs.getInt(key, defaultValue)));
        input.setTextColor(Color.WHITE);
        input.setMinWidth(dp(80));
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    int value = Integer.parseInt(input.getText().toString());
                    prefs.edit().putInt(key, value).apply();
                } catch (NumberFormatException e) {
                    input.setText(String.valueOf(defaultValue));
                    prefs.edit().putInt(key, defaultValue).apply();
                }
                Settings.notifySettingsChanged();
            }
        });
        container.addView(input);

        mainLayout.addView(container);
    }

    private void addButton(String text, Runnable onClick) {
        Button button = new Button(this);
        button.setText(text);
        button.setBackgroundColor(Color.parseColor("#FE2C55")); // TikTok pink
        button.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, dp(8));
        button.setLayoutParams(params);
        button.setOnClickListener(v -> onClick.run());
        mainLayout.addView(button);
    }

    private void syncRemoteConfig() {
        String url = prefs.getString("remote_config_url", "");
        if (url.isEmpty()) {
            showToast("Please enter a config URL first");
            return;
        }

        if (!prefs.getBoolean("remote_config_enabled", false)) {
            showToast("Remote config is disabled");
            return;
        }

        showToast("Syncing...");
        RemoteConfigManager.setConfigUrl(url);
        RemoteConfigManager.setEnabled(true);
        RemoteConfigManager.forceSync(new RemoteConfigManager.ConfigCallback() {
            @Override
            public void onConfigLoaded(RemoteConfig config) {
                runOnUiThread(() -> {
                    showToast("Config synced successfully (version " + config.configVersion + ")");
                    Settings.setLastRemoteConfigSync(System.currentTimeMillis());

                    // Show announcement if available
                    if (config.announcement != null && config.announcement.enabled) {
                        showAnnouncement(config.announcement);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> showToast("Sync failed: " + error));
            }
        });
    }

    private void checkForUpdates() {
        String url = prefs.getString("remote_config_url", "");
        if (url.isEmpty()) {
            showToast("Please enter a config URL first");
            return;
        }

        showToast("Checking for updates...");
        RemoteConfigManager.setConfigUrl(url);
        RemoteConfigManager.checkForUpdates("1.0.0", new RemoteConfigManager.UpdateCheckCallback() {
            @Override
            public void onUpdateAvailable(String version, String updateUrl, String message) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Update Available")
                        .setMessage("Version " + version + " is available.\n\n" + message)
                        .setPositiveButton("Download", (dialog, which) -> {
                            if (updateUrl != null && !updateUrl.isEmpty()) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Later", null)
                        .show();
                });
            }

            @Override
            public void onNoUpdate() {
                runOnUiThread(() -> showToast("You're up to date!"));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> showToast("Check failed: " + error));
            }
        });
    }

    private void showAnnouncement(RemoteConfig.Announcement announcement) {
        if (announcement == null || !announcement.enabled) return;

        // Check if already dismissed
        if (RemoteConfigManager.isAnnouncementDismissed(announcement.dismissKey)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(announcement.title.isEmpty() ? "Announcement" : announcement.title)
            .setMessage(announcement.message);

        if (announcement.url != null && !announcement.url.isEmpty()) {
            builder.setPositiveButton("Learn More", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(announcement.url));
                startActivity(intent);
            });
        }

        builder.setNegativeButton("Dismiss", (dialog, which) -> {
            if (announcement.dismissKey != null && !announcement.dismissKey.isEmpty()) {
                RemoteConfigManager.dismissAnnouncement(announcement.dismissKey);
            }
        });

        builder.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Static method to launch settings from anywhere.
     */
    public static void launch(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}

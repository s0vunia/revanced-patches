package app.revanced.extension.tiktok.banner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Banner Manager - Cloud Mode Style (tigrik-compatible)
 *
 * Config format (4 lines, plain text):
 * Line 1: duration in seconds (re-show delay after dismiss)
 * Line 2: action URL (opened on image click)
 * Line 3: image URL (banner image)
 * Line 4: banner key (changing this forces re-show even if dismissed)
 */
public final class BannerManager {
    private static final String TAG = "TikTokBanner";
    private static final String PREFS_NAME = "tikmod_banner";
    private static final String KEY_DISMISS_TIME = "dismiss_time";
    private static final String KEY_DISMISS_DURATION = "dismiss_duration";
    private static final String KEY_BANNER_KEY = "banner_key";

    private static final long SHOW_DELAY_MS = 3000;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final String CONFIG_URL = "https://raw.githubusercontent.com/s0vunia/banners/main/config.conf";

    private static boolean bannerShown = false;
    private static boolean initialized = false;
    private static Context appContext = null;
    private static AlertDialog currentDialog = null;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void onAppStarted(Context context) {
        try {
            if (context == null || initialized) return;
            initialized = true;
            appContext = context.getApplicationContext();
            Log.d(TAG, "Banner manager initialized");
            fetchAndShowBanner();
        } catch (Exception e) {
            Log.e(TAG, "Error in onAppStarted", e);
        }
    }

    private static void fetchAndShowBanner() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Fetching config from: " + CONFIG_URL);

                HttpURLConnection conn = (HttpURLConnection) new URL(CONFIG_URL).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() != 200) {
                    Log.e(TAG, "Config fetch failed: " + conn.getResponseCode());
                    return;
                }

                BannerConfig config = parseConfig(conn.getInputStream());
                if (config == null) {
                    Log.e(TAG, "Failed to parse config");
                    return;
                }

                Log.d(TAG, "Config: duration=" + config.duration + ", url=" + config.actionUrl);

                if (isDismissed(config)) {
                    Log.d(TAG, "Banner still dismissed");
                    return;
                }

                loadAndShowImage(config);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching config", e);
            }
        });
    }

    /**
     * Parse 4-line config: duration, actionUrl, imageUrl, bannerKey
     */
    private static BannerConfig parseConfig(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line1 = reader.readLine();
            String line2 = reader.readLine();
            String line3 = reader.readLine();
            String line4 = reader.readLine();

            if (line1 == null || line2 == null || line3 == null) return null;

            BannerConfig config = new BannerConfig();
            config.duration = Long.parseLong(line1.trim());
            config.actionUrl = line2.trim();
            config.imageUrl = line3.trim();
            config.bannerKey = (line4 != null) ? line4.trim() : "";

            if (config.imageUrl.isEmpty()) return null;
            return config;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing config", e);
            return null;
        }
    }

    private static void loadAndShowImage(BannerConfig config) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Loading image: " + config.imageUrl);

                HttpURLConnection conn = (HttpURLConnection) new URL(config.imageUrl).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());
                if (bitmap == null) {
                    Log.e(TAG, "Failed to decode image");
                    return;
                }

                Log.d(TAG, "Image loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                mainHandler.postDelayed(() -> showBannerDialog(bitmap, config), SHOW_DELAY_MS);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image", e);
            }
        });
    }

    /**
     * Show banner dialog matching tigrik Cloud Mode interface:
     * AlertDialog with RelativeLayout containing ImageView + Button with countdown
     */
    private static void showBannerDialog(Bitmap bitmap, BannerConfig config) {
        try {
            if (bannerShown) return;

            Activity activity = getCurrentActivity();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                Log.e(TAG, "No valid activity");
                return;
            }

            bannerShown = true;

            // Main container - RelativeLayout
            RelativeLayout container = new RelativeLayout(activity);
            int padding = dpToPx(activity, 0);
            container.setPadding(padding, padding, padding, padding);

            // ImageView - banner image (clickable)
            ImageView imageView = new ImageView(activity);
            imageView.setId(View.generateViewId());
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setAdjustViewBounds(true);

            RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            imageParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            imageView.setLayoutParams(imageParams);

            if (config.actionUrl != null && !config.actionUrl.isEmpty()) {
                imageView.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(config.actionUrl));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening URL", e);
                    }
                });
            }
            container.addView(imageView);

            // Close button with countdown timer
            Button closeButton = new Button(activity);
            closeButton.setId(View.generateViewId());
            closeButton.setEnabled(false);
            closeButton.setAllCaps(false);
            closeButton.setText("Close (" + COUNTDOWN_SECONDS + ")");
            closeButton.setTextColor(Color.WHITE);
            closeButton.setBackgroundColor(Color.argb(180, 50, 50, 50));
            closeButton.setPadding(dpToPx(activity, 16), dpToPx(activity, 8),
                    dpToPx(activity, 16), dpToPx(activity, 8));

            RelativeLayout.LayoutParams btnParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            btnParams.addRule(RelativeLayout.BELOW, imageView.getId());
            btnParams.topMargin = dpToPx(activity, 8);
            closeButton.setLayoutParams(btnParams);

            container.addView(closeButton);

            // Build AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setView(container);
            builder.setCancelable(false);

            AlertDialog dialog = builder.create();
            currentDialog = dialog;

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            dialog.show();
            Log.d(TAG, "Banner displayed!");

            // Start countdown timer
            new CountDownTimer(COUNTDOWN_SECONDS * 1000L, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000 + 1;
                    closeButton.setText("Close (" + seconds + ")");
                }

                @Override
                public void onFinish() {
                    closeButton.setText("Close");
                    closeButton.setEnabled(true);
                    closeButton.setBackgroundColor(Color.argb(220, 60, 60, 60));
                }
            }.start();

            // Close button click - fade out and dismiss
            closeButton.setOnClickListener(v -> dismissWithAnimation(dialog, container, config));

        } catch (Exception e) {
            Log.e(TAG, "Error showing banner", e);
        }
    }

    /**
     * Dismiss dialog with fade-out animation
     */
    private static void dismissWithAnimation(AlertDialog dialog, View container, BannerConfig config) {
        try {
            AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeOut.setDuration(300);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    try {
                        markDismissed(config);
                        dialog.dismiss();
                        currentDialog = null;
                    } catch (Exception e) {
                        Log.e(TAG, "Error dismissing", e);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            container.startAnimation(fadeOut);
        } catch (Exception e) {
            try {
                markDismissed(config);
                dialog.dismiss();
            } catch (Exception ignored) {}
        }
    }

    private static Activity getCurrentActivity() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Object activities = activitiesField.get(activityThread);
            if (activities instanceof java.util.Map) {
                for (Object activityRecord : ((java.util.Map<?, ?>) activities).values()) {
                    Class<?> activityRecordClass = activityRecord.getClass();
                    java.lang.reflect.Field pausedField = activityRecordClass.getDeclaredField("paused");
                    pausedField.setAccessible(true);

                    if (!pausedField.getBoolean(activityRecord)) {
                        java.lang.reflect.Field activityField = activityRecordClass.getDeclaredField("activity");
                        activityField.setAccessible(true);
                        return (Activity) activityField.get(activityRecord);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting activity", e);
        }
        return null;
    }

    private static boolean isDismissed(BannerConfig config) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedKey = prefs.getString(KEY_BANNER_KEY, "");
        long dismissTime = prefs.getLong(KEY_DISMISS_TIME, 0);
        long dismissDuration = prefs.getLong(KEY_DISMISS_DURATION, 0);

        if (!config.bannerKey.isEmpty() && !config.bannerKey.equals(savedKey)) {
            Log.d(TAG, "New banner key: " + config.bannerKey + " (was: " + savedKey + ")");
            return false;
        }

        if (dismissTime == 0) return false;

        long elapsed = (System.currentTimeMillis() - dismissTime) / 1000;
        return elapsed < dismissDuration;
    }

    private static void markDismissed(BannerConfig config) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_DISMISS_TIME, System.currentTimeMillis())
                .putLong(KEY_DISMISS_DURATION, config.duration)
                .putString(KEY_BANNER_KEY, config.bannerKey)
                .apply();
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private static class BannerConfig {
        long duration;
        String actionUrl;
        String imageUrl;
        String bannerKey;
    }
}

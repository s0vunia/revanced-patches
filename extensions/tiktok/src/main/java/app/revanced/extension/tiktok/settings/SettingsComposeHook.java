package app.revanced.extension.tiktok.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bytedance.ies.ugc.aweme.commercialize.compliance.personalization.AdPersonalizationActivity;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.tiktok.settings.preference.TikTokPreferenceFragment;

/**
 * Hook for TikTok 42+ Compose-based settings UI.
 * Injects a "Tralalelo Settings" button at the top of the settings screen.
 */
@SuppressWarnings("unused")
public class SettingsComposeHook {

    /**
     * Called from onViewCreated to inject the Tralalelo settings button into the view hierarchy.
     *
     * @param view The root view from onViewCreated (the ComposeView)
     */
    public static void onViewCreated(View view) {
        try {
            if (view == null) {
                Logger.printDebug(() -> "onViewCreated: view is null");
                return;
            }

            Context context = view.getContext();
            ViewGroup parent = (ViewGroup) view.getParent();

            if (parent == null) {
                Logger.printDebug(() -> "onViewCreated: parent is null");
                return;
            }

            Logger.printDebug(() -> "onViewCreated: injecting Tralalelo button, parent=" + parent.getClass().getName());

            // Create Tralalelo settings button
            TextView settingsButton = new TextView(context);
            settingsButton.setText("⚙ Tralalelo Settings");
            settingsButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            settingsButton.setTypeface(null, Typeface.BOLD);
            settingsButton.setTextColor(Color.WHITE);
            settingsButton.setBackgroundColor(Color.parseColor("#FE2C55")); // TikTok red
            settingsButton.setGravity(Gravity.CENTER);
            settingsButton.setPadding(48, 48, 48, 48);

            // Use FrameLayout.LayoutParams to position at top
            FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            buttonParams.setMargins(32, 32, 32, 0);
            buttonParams.gravity = Gravity.TOP;
            settingsButton.setLayoutParams(buttonParams);
            settingsButton.setElevation(10f);  // Make sure it's on top

            settingsButton.setOnClickListener(v -> openTralaSettings(context));

            // Add button to parent (usually a FrameLayout)
            parent.addView(settingsButton);

            Logger.printDebug(() -> "Successfully added Tralalelo button to settings");
        } catch (Exception e) {
            Logger.printException(() -> "Failed to inject settings button", e);
        }
    }

    /**
     * Opens the Tralalelo settings activity.
     */
    public static void openTralaSettings(Context context) {
        try {
            Intent intent = new Intent(context, AdPersonalizationActivity.class);
            intent.putExtra("revanced", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Logger.printDebug(() -> "Opening Tralalelo settings");
        } catch (Exception e) {
            Logger.printException(() -> "Failed to open Tralalelo settings", e);
        }
    }

    /**
     * Initializes the settings menu when AdPersonalizationActivity is opened with revanced=true.
     *
     * @param activity The AdPersonalizationActivity instance
     * @return true if we handled it (show our settings), false to show normal activity
     */
    public static boolean initializeSettings(AdPersonalizationActivity activity) {
        try {
            Bundle extras = activity.getIntent().getExtras();
            if (extras == null || !extras.getBoolean("revanced", false)) {
                return false; // Not our settings, let normal flow continue
            }

            Logger.printDebug(() -> "Initializing Tralalelo settings");
            SettingsStatus.load();

            // Create settings UI
            LinearLayout linearLayout = new LinearLayout(activity);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setFitsSystemWindows(true);
            linearLayout.setTransitionGroup(true);

            FrameLayout fragment = new FrameLayout(activity);
            fragment.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            int fragmentId = View.generateViewId();
            fragment.setId(fragmentId);

            linearLayout.addView(fragment);
            activity.setContentView(linearLayout);

            TikTokPreferenceFragment preferenceFragment = new TikTokPreferenceFragment();
            activity.getFragmentManager().beginTransaction()
                .replace(fragmentId, preferenceFragment)
                .commit();

            return true;
        } catch (Exception e) {
            Logger.printException(() -> "Failed to initialize settings", e);
            return false;
        }
    }
}

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
     * Wraps the settings ComposeView and adds our Tralalelo settings button at the top.
     *
     * @param composeView The original ComposeView from SettingsComposeVersionFragment
     * @return A LinearLayout containing our button + the original ComposeView
     */
    public static View wrapSettingsView(View composeView) {
        try {
            Context context = composeView.getContext();

            // Create container LinearLayout
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));

            // Create Tralalelo settings button
            TextView settingsButton = new TextView(context);
            settingsButton.setText("Tralalelo Settings");
            settingsButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            settingsButton.setTypeface(null, Typeface.BOLD);
            settingsButton.setTextColor(Color.WHITE);
            settingsButton.setBackgroundColor(Color.parseColor("#FE2C55")); // TikTok red
            settingsButton.setGravity(Gravity.CENTER);
            settingsButton.setPadding(48, 36, 48, 36);

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            buttonParams.setMargins(24, 24, 24, 12);
            settingsButton.setLayoutParams(buttonParams);

            settingsButton.setOnClickListener(v -> openTralaSettings(context));

            // Add button first, then compose view
            container.addView(settingsButton);

            // Remove composeView from its parent if it has one
            if (composeView.getParent() != null) {
                ((ViewGroup) composeView.getParent()).removeView(composeView);
            }
            container.addView(composeView);

            Logger.printDebug(() -> "Successfully wrapped settings view with Tralalelo button");
            return container;
        } catch (Exception e) {
            Logger.printException(() -> "Failed to wrap settings view", e);
            return composeView; // Return original if wrapping fails
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

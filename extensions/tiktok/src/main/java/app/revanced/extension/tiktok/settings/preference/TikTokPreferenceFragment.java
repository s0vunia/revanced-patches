package app.revanced.extension.tiktok.settings.preference;

import android.preference.Preference;
import android.preference.PreferenceScreen;

import androidx.annotation.NonNull;

import app.revanced.extension.shared.Utils;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment;
import app.revanced.extension.tiktok.settings.preference.categories.ExtensionPreferenceCategory;
import app.revanced.extension.tiktok.settings.preference.categories.FeedFilterPreferenceCategory;
import app.revanced.extension.tiktok.settings.preference.categories.SimSpoofPreferenceCategory;

/**
 * Preference fragment for ReVanced settings
 */
@SuppressWarnings("deprecation")
public class TikTokPreferenceFragment extends AbstractPreferenceFragment {

    @Override
    protected void syncSettingWithPreference(@NonNull Preference pref,
                                             @NonNull Setting<?> setting,
                                             boolean applySettingToPreference) {
        if (pref instanceof RangeValuePreference) {
            RangeValuePreference rangeValuePref = (RangeValuePreference) pref;
            Setting.privateSetValueFromString(setting, rangeValuePref.getValue());
        } else {
            super.syncSettingWithPreference(pref, setting, applySettingToPreference);
        }
    }

    @Override
    protected void initialize() {
        final var context = getActivity();

        // Currently no resources can be compiled for TikTok (fails with aapt error).
        // So all TikTok Strings are hard coded in the extension.
        restartDialogTitle = "Требуется перезапуск";
        restartDialogMessage = "Перезапустите приложение для применения изменений.";
        restartDialogButtonText = "Перезапустить";
        confirmDialogTitle = "Вы хотите продолжить?";

        // App does not use dark mode.
        Utils.setIsDarkModeEnabled(false);

        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(preferenceScreen);

        // Custom categories reference app specific Settings class.
        new FeedFilterPreferenceCategory(context, preferenceScreen);
        new SimSpoofPreferenceCategory(context, preferenceScreen);
        new ExtensionPreferenceCategory(context, preferenceScreen);
    }
}

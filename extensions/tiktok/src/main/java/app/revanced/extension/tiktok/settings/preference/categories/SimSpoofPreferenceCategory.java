package app.revanced.extension.tiktok.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;
import app.revanced.extension.tiktok.settings.Settings;
import app.revanced.extension.tiktok.settings.SettingsStatus;
import app.revanced.extension.tiktok.settings.preference.InputTextPreference;
import app.revanced.extension.tiktok.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class SimSpoofPreferenceCategory extends ConditionalPreferenceCategory {
    public SimSpoofPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Обход региональных ограничений");
    }


    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.simSpoofEnabled;
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new TogglePreference(
                context,
                "Подмена SIM-карты",
                "Обход региональных ограничений через подмену данных SIM.",
                Settings.SIM_SPOOF
        ));
        addPreference(new InputTextPreference(
                context,
                "Код страны ISO", "us, uk, jp, ...",
                Settings.SIM_SPOOF_ISO
        ));
        addPreference(new InputTextPreference(
                context,
                "MCC+MNC оператора", "mcc+mnc",
                Settings.SIMSPOOF_MCCMNC
        ));
        addPreference(new InputTextPreference(
                context,
                "Имя оператора", "Название оператора связи.",
                Settings.SIMSPOOF_OP_NAME
        ));
    }
}

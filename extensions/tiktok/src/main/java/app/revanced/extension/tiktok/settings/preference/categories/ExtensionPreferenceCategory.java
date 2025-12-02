package app.revanced.extension.tiktok.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.tiktok.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class ExtensionPreferenceCategory extends ConditionalPreferenceCategory {
    public ExtensionPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Разное");
    }

    @Override
    public boolean getSettingsStatus() {
        return true;
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new TogglePreference(context,
                "Очистка ссылок",
                "Удалять параметры отслеживания из ссылок.",
                BaseSettings.SANITIZE_SHARED_LINKS
        ));

        addPreference(new TogglePreference(context,
                "Режим отладки",
                "Показывать отладочные логи.",
                BaseSettings.DEBUG
        ));
    }
}

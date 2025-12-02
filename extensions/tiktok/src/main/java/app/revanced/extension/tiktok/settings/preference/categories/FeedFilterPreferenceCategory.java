package app.revanced.extension.tiktok.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;
import app.revanced.extension.tiktok.settings.preference.RangeValuePreference;
import app.revanced.extension.tiktok.settings.Settings;
import app.revanced.extension.tiktok.settings.SettingsStatus;
import app.revanced.extension.tiktok.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class FeedFilterPreferenceCategory extends ConditionalPreferenceCategory {
    public FeedFilterPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Фильтр ленты");
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.feedFilterEnabled;
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new TogglePreference(
                context,
                "Убрать рекламу", "Удалить рекламу из ленты.",
                Settings.REMOVE_ADS
        ));
        addPreference(new TogglePreference(
                context,
                "Скрыть TikTok Shop", "Скрыть магазин TikTok из ленты.",
                Settings.HIDE_SHOP
        ));
        addPreference(new TogglePreference(
                context,
                "Скрыть трансляции", "Скрыть прямые трансляции из ленты.",
                Settings.HIDE_LIVE
        ));
        addPreference(new TogglePreference(
                context,
                "Скрыть истории", "Скрыть истории из ленты.",
                Settings.HIDE_STORY
        ));
        addPreference(new TogglePreference(
                context,
                "Скрыть фото-видео", "Скрыть видео из фото из ленты.",
                Settings.HIDE_IMAGE
        ));
        addPreference(new RangeValuePreference(
                context,
                "Мин/Макс просмотры", "Минимальное или максимальное количество просмотров видео.",
                Settings.MIN_MAX_VIEWS
        ));
        addPreference(new RangeValuePreference(
                context,
                "Мин/Макс лайки", "Минимальное или максимальное количество лайков видео.",
                Settings.MIN_MAX_LIKES
        ));
    }
}

/**
 * Settings Persistence - Local Only (NO CLOUD)
 *
 * Saves and loads settings from SharedPreferences.
 * All settings are stored locally on the device.
 */

#include "tikmod.h"

namespace Settings {

static const char* PREFS_NAME = "tikmod_settings";

jobject getSharedPreferences(JNIEnv* env, jobject context) {
    if (context == nullptr) return nullptr;

    jclass contextClass = env->GetObjectClass(context);
    jmethodID getPrefs = env->GetMethodID(contextClass, "getSharedPreferences",
        "(Ljava/lang/String;I)Landroid/content/SharedPreferences;");

    if (getPrefs == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }

    jstring prefsName = env->NewStringUTF(PREFS_NAME);
    jobject prefs = env->CallObjectMethod(context, getPrefs, prefsName, 0);
    env->DeleteLocalRef(prefsName);

    return prefs;
}

jobject getEditor(JNIEnv* env, jobject prefs) {
    if (prefs == nullptr) return nullptr;

    jclass prefsClass = env->GetObjectClass(prefs);
    jmethodID edit = env->GetMethodID(prefsClass, "edit",
        "()Landroid/content/SharedPreferences$Editor;");

    if (edit == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }

    return env->CallObjectMethod(prefs, edit);
}

void applyEditor(JNIEnv* env, jobject editor) {
    if (editor == nullptr) return;

    jclass editorClass = env->GetObjectClass(editor);
    jmethodID apply = env->GetMethodID(editorClass, "apply", "()V");

    if (apply != nullptr) {
        env->CallVoidMethod(editor, apply);
    }
}

bool getBoolean(JNIEnv* env, jobject prefs, const char* key, bool defaultValue) {
    if (prefs == nullptr) return defaultValue;

    jclass prefsClass = env->GetObjectClass(prefs);
    jmethodID getBool = env->GetMethodID(prefsClass, "getBoolean", "(Ljava/lang/String;Z)Z");

    if (getBool == nullptr) {
        env->ExceptionClear();
        return defaultValue;
    }

    jstring keyStr = env->NewStringUTF(key);
    jboolean result = env->CallBooleanMethod(prefs, getBool, keyStr, defaultValue ? JNI_TRUE : JNI_FALSE);
    env->DeleteLocalRef(keyStr);

    return result == JNI_TRUE;
}

void setBoolean(JNIEnv* env, jobject editor, const char* key, bool value) {
    if (editor == nullptr) return;

    jclass editorClass = env->GetObjectClass(editor);
    jmethodID putBool = env->GetMethodID(editorClass, "putBoolean",
        "(Ljava/lang/String;Z)Landroid/content/SharedPreferences$Editor;");

    if (putBool == nullptr) {
        env->ExceptionClear();
        return;
    }

    jstring keyStr = env->NewStringUTF(key);
    env->CallObjectMethod(editor, putBool, keyStr, value ? JNI_TRUE : JNI_FALSE);
    env->DeleteLocalRef(keyStr);
}

std::string getString(JNIEnv* env, jobject prefs, const char* key, const char* defaultValue) {
    if (prefs == nullptr) return defaultValue;

    jclass prefsClass = env->GetObjectClass(prefs);
    jmethodID getStr = env->GetMethodID(prefsClass, "getString",
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");

    if (getStr == nullptr) {
        env->ExceptionClear();
        return defaultValue;
    }

    jstring keyStr = env->NewStringUTF(key);
    jstring defStr = env->NewStringUTF(defaultValue);
    jstring result = (jstring)env->CallObjectMethod(prefs, getStr, keyStr, defStr);
    env->DeleteLocalRef(keyStr);
    env->DeleteLocalRef(defStr);

    if (result == nullptr) return defaultValue;

    return Utils::jstringToString(env, result);
}

void setString(JNIEnv* env, jobject editor, const char* key, const std::string& value) {
    if (editor == nullptr) return;

    jclass editorClass = env->GetObjectClass(editor);
    jmethodID putStr = env->GetMethodID(editorClass, "putString",
        "(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences$Editor;");

    if (putStr == nullptr) {
        env->ExceptionClear();
        return;
    }

    jstring keyStr = env->NewStringUTF(key);
    jstring valStr = env->NewStringUTF(value.c_str());
    env->CallObjectMethod(editor, putStr, keyStr, valStr);
    env->DeleteLocalRef(keyStr);
    env->DeleteLocalRef(valStr);
}

float getFloat(JNIEnv* env, jobject prefs, const char* key, float defaultValue) {
    if (prefs == nullptr) return defaultValue;

    jclass prefsClass = env->GetObjectClass(prefs);
    jmethodID getF = env->GetMethodID(prefsClass, "getFloat", "(Ljava/lang/String;F)F");

    if (getF == nullptr) {
        env->ExceptionClear();
        return defaultValue;
    }

    jstring keyStr = env->NewStringUTF(key);
    jfloat result = env->CallFloatMethod(prefs, getF, keyStr, defaultValue);
    env->DeleteLocalRef(keyStr);

    return result;
}

void setFloat(JNIEnv* env, jobject editor, const char* key, float value) {
    if (editor == nullptr) return;

    jclass editorClass = env->GetObjectClass(editor);
    jmethodID putF = env->GetMethodID(editorClass, "putFloat",
        "(Ljava/lang/String;F)Landroid/content/SharedPreferences$Editor;");

    if (putF == nullptr) {
        env->ExceptionClear();
        return;
    }

    jstring keyStr = env->NewStringUTF(key);
    env->CallObjectMethod(editor, putF, keyStr, value);
    env->DeleteLocalRef(keyStr);
}

long getLong(JNIEnv* env, jobject prefs, const char* key, long defaultValue) {
    if (prefs == nullptr) return defaultValue;

    jclass prefsClass = env->GetObjectClass(prefs);
    jmethodID getL = env->GetMethodID(prefsClass, "getLong", "(Ljava/lang/String;J)J");

    if (getL == nullptr) {
        env->ExceptionClear();
        return defaultValue;
    }

    jstring keyStr = env->NewStringUTF(key);
    jlong result = env->CallLongMethod(prefs, getL, keyStr, (jlong)defaultValue);
    env->DeleteLocalRef(keyStr);

    return (long)result;
}

void setLong(JNIEnv* env, jobject editor, const char* key, long value) {
    if (editor == nullptr) return;

    jclass editorClass = env->GetObjectClass(editor);
    jmethodID putL = env->GetMethodID(editorClass, "putLong",
        "(Ljava/lang/String;J)Landroid/content/SharedPreferences$Editor;");

    if (putL == nullptr) {
        env->ExceptionClear();
        return;
    }

    jstring keyStr = env->NewStringUTF(key);
    env->CallObjectMethod(editor, putL, keyStr, (jlong)value);
    env->DeleteLocalRef(keyStr);
}

void load(JNIEnv* env, jobject context) {
    LOGI("Loading settings...");

    jobject prefs = getSharedPreferences(env, context);
    if (prefs == nullptr) {
        LOGE("Failed to get SharedPreferences");
        return;
    }

    // Watermark settings
    g_settings.removeVideoWatermark = getBoolean(env, prefs, "remove_video_watermark", true);
    g_settings.removePicWatermark = getBoolean(env, prefs, "remove_pic_watermark", true);
    g_settings.removeCommentPicWatermark = getBoolean(env, prefs, "remove_comment_pic_watermark", true);

    // Download settings
    g_settings.enableDownload = getBoolean(env, prefs, "enable_download", true);
    g_settings.downloadViaTelegram = getBoolean(env, prefs, "download_via_telegram", false);
    g_settings.singleImageModeDownload = getBoolean(env, prefs, "single_image_mode_download", false);

    // Telegram bot settings
    g_settings.telegramBotUsername = getString(env, prefs, "telegram_bot_username", "instagramavBot");
    g_settings.includeVideoId = getBoolean(env, prefs, "include_video_id", true);
    g_settings.customTelegramDeeplink = getString(env, prefs, "custom_telegram_deeplink", "");

    // SDK blocking
    g_settings.blockAdSdk = getBoolean(env, prefs, "block_ad_sdk", true);
    g_settings.blockAnalytics = getBoolean(env, prefs, "block_analytics", true);
    g_settings.removeStories = getBoolean(env, prefs, "remove_stories", false);
    g_settings.removeShop = getBoolean(env, prefs, "remove_shop", true);

    // Feed filtering
    g_settings.hideAds = getBoolean(env, prefs, "hide_ads", true);
    g_settings.hideLiveStreams = getBoolean(env, prefs, "hide_live_streams", false);
    g_settings.hideImagePosts = getBoolean(env, prefs, "hide_image_posts", false);
    g_settings.hideLongPosts = getBoolean(env, prefs, "hide_long_posts", false);
    g_settings.hidePromotionalMusic = getBoolean(env, prefs, "hide_promotional_music", true);
    g_settings.filterByViews = getBoolean(env, prefs, "filter_by_views", false);
    g_settings.minViewsCount = getLong(env, prefs, "min_views_count", 0);
    g_settings.filterByLikes = getBoolean(env, prefs, "filter_by_likes", false);
    g_settings.minLikesCount = getLong(env, prefs, "min_likes_count", 0);

    // Region settings
    g_settings.forceRegion = getBoolean(env, prefs, "force_region", false);
    g_settings.forcedRegion = getString(env, prefs, "forced_region", "");

    // UI settings
    g_settings.rememberPlaybackSpeed = getBoolean(env, prefs, "remember_playback_speed", true);
    g_settings.playbackSpeed = getFloat(env, prefs, "playback_speed", 1.0f);
    g_settings.clearDisplayMode = getBoolean(env, prefs, "clear_display_mode", false);

    // Ad removal
    g_settings.removeSpotlightAds = getBoolean(env, prefs, "remove_spotlight_ads", true);
    g_settings.removeDiscoverAds = getBoolean(env, prefs, "remove_discover_ads", true);

    env->DeleteLocalRef(prefs);
    LOGI("Settings loaded successfully");
}

void save(JNIEnv* env, jobject context) {
    LOGD("Saving settings...");

    jobject prefs = getSharedPreferences(env, context);
    if (prefs == nullptr) {
        LOGE("Failed to get SharedPreferences for saving");
        return;
    }

    jobject editor = getEditor(env, prefs);
    if (editor == nullptr) {
        env->DeleteLocalRef(prefs);
        LOGE("Failed to get Editor");
        return;
    }

    // Watermark settings
    setBoolean(env, editor, "remove_video_watermark", g_settings.removeVideoWatermark);
    setBoolean(env, editor, "remove_pic_watermark", g_settings.removePicWatermark);
    setBoolean(env, editor, "remove_comment_pic_watermark", g_settings.removeCommentPicWatermark);

    // Download settings
    setBoolean(env, editor, "enable_download", g_settings.enableDownload);
    setBoolean(env, editor, "download_via_telegram", g_settings.downloadViaTelegram);
    setBoolean(env, editor, "single_image_mode_download", g_settings.singleImageModeDownload);

    // Telegram bot settings
    setString(env, editor, "telegram_bot_username", g_settings.telegramBotUsername);
    setBoolean(env, editor, "include_video_id", g_settings.includeVideoId);
    setString(env, editor, "custom_telegram_deeplink", g_settings.customTelegramDeeplink);

    // SDK blocking
    setBoolean(env, editor, "block_ad_sdk", g_settings.blockAdSdk);
    setBoolean(env, editor, "block_analytics", g_settings.blockAnalytics);
    setBoolean(env, editor, "remove_stories", g_settings.removeStories);
    setBoolean(env, editor, "remove_shop", g_settings.removeShop);

    // Feed filtering
    setBoolean(env, editor, "hide_ads", g_settings.hideAds);
    setBoolean(env, editor, "hide_live_streams", g_settings.hideLiveStreams);
    setBoolean(env, editor, "hide_image_posts", g_settings.hideImagePosts);
    setBoolean(env, editor, "hide_long_posts", g_settings.hideLongPosts);
    setBoolean(env, editor, "hide_promotional_music", g_settings.hidePromotionalMusic);
    setBoolean(env, editor, "filter_by_views", g_settings.filterByViews);
    setLong(env, editor, "min_views_count", g_settings.minViewsCount);
    setBoolean(env, editor, "filter_by_likes", g_settings.filterByLikes);
    setLong(env, editor, "min_likes_count", g_settings.minLikesCount);

    // Region settings
    setBoolean(env, editor, "force_region", g_settings.forceRegion);
    setString(env, editor, "forced_region", g_settings.forcedRegion);

    // UI settings
    setBoolean(env, editor, "remember_playback_speed", g_settings.rememberPlaybackSpeed);
    setFloat(env, editor, "playback_speed", g_settings.playbackSpeed);
    setBoolean(env, editor, "clear_display_mode", g_settings.clearDisplayMode);

    // Ad removal
    setBoolean(env, editor, "remove_spotlight_ads", g_settings.removeSpotlightAds);
    setBoolean(env, editor, "remove_discover_ads", g_settings.removeDiscoverAds);

    applyEditor(env, editor);

    env->DeleteLocalRef(editor);
    env->DeleteLocalRef(prefs);
    LOGD("Settings saved successfully");
}

} // namespace Settings

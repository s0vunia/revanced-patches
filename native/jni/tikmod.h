#ifndef TIKMOD_H
#define TIKMOD_H

#include <jni.h>
#include <android/log.h>
#include <string>
#include <unordered_map>
#include <mutex>

#define LOG_TAG "TikMod"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global JVM reference
extern JavaVM* g_jvm;
extern jobject g_context;

// ============================================================
// Settings Structure - NO CLOUD CONTROL
// ============================================================
struct ModSettings {
    // Watermark settings
    bool removeVideoWatermark = true;
    bool removePicWatermark = true;
    bool removeCommentPicWatermark = true;

    // Download settings
    bool enableDownload = true;
    bool downloadViaTelegram = false;
    bool singleImageModeDownload = false;

    // Telegram bot settings
    std::string telegramBotUsername = "instagramavBot";
    bool includeVideoId = true;
    std::string customTelegramDeeplink = "";

    // SDK blocking
    bool blockAdSdk = true;
    bool blockAnalytics = true;
    bool removeStories = false;
    bool removeShop = true;

    // Feed filtering
    bool hideAds = true;
    bool hideLiveStreams = false;
    bool hideImagePosts = false;
    bool hideLongPosts = false;
    bool hidePromotionalMusic = true;
    bool filterByViews = false;
    long minViewsCount = 0;
    bool filterByLikes = false;
    long minLikesCount = 0;

    // Region settings
    bool forceRegion = false;
    std::string forcedRegion = "";

    // UI settings
    bool rememberPlaybackSpeed = true;
    float playbackSpeed = 1.0f;
    bool clearDisplayMode = false;

    // Misc
    bool removeSpotlightAds = true;
    bool removeDiscoverAds = true;
};

extern ModSettings g_settings;

// ============================================================
// Settings Functions
// ============================================================
namespace Settings {
    void load(JNIEnv* env, jobject context);
    void save(JNIEnv* env, jobject context);
    bool getBoolean(JNIEnv* env, jobject prefs, const char* key, bool defaultValue);
    void setBoolean(JNIEnv* env, jobject editor, const char* key, bool value);
    std::string getString(JNIEnv* env, jobject prefs, const char* key, const char* defaultValue);
    void setString(JNIEnv* env, jobject editor, const char* key, const std::string& value);
    float getFloat(JNIEnv* env, jobject prefs, const char* key, float defaultValue);
    void setFloat(JNIEnv* env, jobject editor, const char* key, float value);
    long getLong(JNIEnv* env, jobject prefs, const char* key, long defaultValue);
    void setLong(JNIEnv* env, jobject editor, const char* key, long value);
}

// ============================================================
// Feed Processing Functions
// ============================================================
namespace FeedProcessor {
    void processAwemeList(JNIEnv* env, jobject feedItemList);
    void processDiscoverBanners(JNIEnv* env, jobject bannerList);
    void processDiscoverTopics(JNIEnv* env, jobject topicList);
    bool shouldFilterAweme(JNIEnv* env, jobject aweme);
    bool isAdContent(JNIEnv* env, jobject aweme);
    bool isLiveStream(JNIEnv* env, jobject aweme);
    bool isImagePost(JNIEnv* env, jobject aweme);
    bool isLongPost(JNIEnv* env, jobject aweme);
    bool hasPromotionalMusic(JNIEnv* env, jobject aweme);
    bool passesViewsFilter(JNIEnv* env, jobject aweme);
    bool passesLikesFilter(JNIEnv* env, jobject aweme);
}

// ============================================================
// Watermark Functions
// ============================================================
namespace Watermark {
    bool getVideoWatermark();
    bool getPicWatermark();
    bool getCommentPicWatermark();
    void setVideoWatermark(bool enabled);
    void setPicWatermark(bool enabled);
    void setCommentPicWatermark(bool enabled);
}

// ============================================================
// Download Functions
// ============================================================
namespace Download {
    bool isDownloadEnabled();
    bool isDownloadViaTelegram();
    bool isSingleImageModeDownload();
    void setDownloadEnabled(bool enabled);
    void setDownloadViaTelegram(bool enabled);
    void setSingleImageModeDownload(bool enabled);
}

// ============================================================
// Hook Registration
// ============================================================
namespace Hooks {
    bool registerNatives(JNIEnv* env, int classIndex, jclass clazz);
    void initializeHooks(JNIEnv* env);
}

// ============================================================
// Utility Functions
// ============================================================
namespace Utils {
    JNIEnv* getEnv();
    std::string jstringToString(JNIEnv* env, jstring str);
    jstring stringToJstring(JNIEnv* env, const std::string& str);
    jobject getApplicationContext(JNIEnv* env);
}

#endif // TIKMOD_H

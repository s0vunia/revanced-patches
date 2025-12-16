/**
 * TikMod - Clean TikTok Mod Library
 *
 * A replacement for libtigrik.so WITHOUT cloud control features.
 * This library provides:
 * - Watermark removal
 * - Ad blocking
 * - Download features
 * - Feed filtering
 * - Local-only settings
 *
 * NO remote kill switch, NO banner fetching, NO telemetry.
 */

#include "tikmod.h"
#include <cstring>

// Global variables
JavaVM* g_jvm = nullptr;
jobject g_context = nullptr;
ModSettings g_settings;

// ============================================================
// JNI_OnLoad - Library Entry Point
// ============================================================
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("TikMod loading...");

    g_jvm = vm;
    JNIEnv* env = nullptr;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return JNI_ERR;
    }

    // Initialize hooks
    Hooks::initializeHooks(env);

    LOGI("TikMod loaded successfully (NO CLOUD CONTROL)");
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGI("TikMod unloading...");

    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_OK) {
        if (g_context != nullptr) {
            env->DeleteGlobalRef(g_context);
            g_context = nullptr;
        }
    }
}

// ============================================================
// Native Method: registerNativesForClass
// This is the main entry point called by Java code
// ============================================================
extern "C" JNIEXPORT void JNICALL
Java_tigrik0_tigrik_registerNativesForClass(JNIEnv* env, jclass clazz, jint classIndex, jclass targetClass) {
    LOGD("registerNativesForClass called for index: %d", classIndex);

    // Store context on first call
    if (g_context == nullptr && classIndex == 0x40) {
        // Class index 0x40 (64) is typically me.tigrik.f.a which has Context access
        // We'll get context from the first available source
    }

    // Register native methods for the specified class
    Hooks::registerNatives(env, classIndex, targetClass);
}

// ============================================================
// Utility Functions Implementation
// ============================================================
namespace Utils {

JNIEnv* getEnv() {
    JNIEnv* env = nullptr;
    if (g_jvm != nullptr) {
        int status = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
        if (status == JNI_EDETACHED) {
            g_jvm->AttachCurrentThread(&env, nullptr);
        }
    }
    return env;
}

std::string jstringToString(JNIEnv* env, jstring str) {
    if (str == nullptr) return "";

    const char* chars = env->GetStringUTFChars(str, nullptr);
    if (chars == nullptr) return "";

    std::string result(chars);
    env->ReleaseStringUTFChars(str, chars);
    return result;
}

jstring stringToJstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

jobject getApplicationContext(JNIEnv* env) {
    if (g_context != nullptr) {
        return g_context;
    }

    // Try to get context via ActivityThread
    jclass activityThreadClass = env->FindClass("android/app/ActivityThread");
    if (activityThreadClass == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }

    jmethodID currentActivityThread = env->GetStaticMethodID(
        activityThreadClass, "currentActivityThread", "()Landroid/app/ActivityThread;");
    if (currentActivityThread == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }

    jobject at = env->CallStaticObjectMethod(activityThreadClass, currentActivityThread);
    if (at == nullptr) {
        return nullptr;
    }

    jmethodID getApplication = env->GetMethodID(
        activityThreadClass, "getApplication", "()Landroid/app/Application;");
    if (getApplication == nullptr) {
        env->ExceptionClear();
        return nullptr;
    }

    jobject context = env->CallObjectMethod(at, getApplication);
    if (context != nullptr) {
        g_context = env->NewGlobalRef(context);
    }

    return g_context;
}

} // namespace Utils

// ============================================================
// Watermark Settings Native Methods
// These replace the original me.tigrik.f.a natives
// ============================================================

// Native for ClientSettings$Watermark.getVideoWatermark
extern "C" JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getVideoWatermark(JNIEnv* env, jclass clazz) {
    return g_settings.removeVideoWatermark ? JNI_FALSE : JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getPicWatermark(JNIEnv* env, jclass clazz) {
    return g_settings.removePicWatermark ? JNI_FALSE : JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getCommentPicWatermark(JNIEnv* env, jclass clazz) {
    return g_settings.removeCommentPicWatermark ? JNI_FALSE : JNI_TRUE;
}

// ============================================================
// Download Settings Native Methods
// ============================================================

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ClientSettings_00024DownloadViaTelegram_getDownloadViaTelegram(JNIEnv* env, jclass clazz) {
    return g_settings.downloadViaTelegram ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ClientSettings_00024SingleImageModeDownload_getSingleImageModeDownload(JNIEnv* env, jclass clazz) {
    return g_settings.singleImageModeDownload ? JNI_TRUE : JNI_FALSE;
}

// ============================================================
// Feed Processing Native Methods
// ============================================================

// Process feed item list - remove ads and apply filters
extern "C" JNIEXPORT void JNICALL
Java_com_tiktok_plugin_client_feed_FeedItemsProcess_processFeedItems(JNIEnv* env, jclass clazz, jobject feedItemList) {
    if (feedItemList == nullptr) return;
    FeedProcessor::processAwemeList(env, feedItemList);
}

// Process discover banner list - remove ad banners
extern "C" JNIEXPORT void JNICALL
Java_com_tiktok_plugin_client_feed_DiscoverProcess_discoverBannerAds(JNIEnv* env, jclass clazz, jobject bannerList) {
    if (bannerList == nullptr || !g_settings.removeDiscoverAds) return;
    FeedProcessor::processDiscoverBanners(env, bannerList);
}

// ============================================================
// Playback Speed Native Methods
// ============================================================

extern "C" JNIEXPORT jfloat JNICALL
Java_com_tiktok_plugin_client_ui_RememberPlaybackSpeed_getPlaybackSpeed(JNIEnv* env, jclass clazz) {
    return g_settings.playbackSpeed;
}

extern "C" JNIEXPORT void JNICALL
Java_com_tiktok_plugin_client_ui_RememberPlaybackSpeed_setPlaybackSpeed(JNIEnv* env, jclass clazz, jfloat speed) {
    g_settings.playbackSpeed = speed;

    // Save to preferences
    jobject context = Utils::getApplicationContext(env);
    if (context != nullptr) {
        Settings::save(env, context);
    }
}

// ============================================================
// Clear Display Native Methods
// ============================================================

extern "C" JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ui_RememberClearDisplay_getClearDisplayState(JNIEnv* env, jclass clazz) {
    return g_settings.clearDisplayMode ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_tiktok_plugin_client_ui_RememberClearDisplay_setClearDisplayState(JNIEnv* env, jclass clazz, jboolean state) {
    g_settings.clearDisplayMode = (state == JNI_TRUE);
}

// ============================================================
// Settings Context Initialization
// ============================================================

extern "C" JNIEXPORT void JNICALL
Java_me_tikmod_Settings_setContext(JNIEnv* env, jclass clazz, jobject context) {
    if (context != nullptr) {
        if (g_context != nullptr) {
            env->DeleteGlobalRef(g_context);
        }
        g_context = env->NewGlobalRef(context);
        Settings::load(env, context);
        LOGI("Settings context initialized");
    }
}

// ============================================================
// Settings Reload (called from Java when settings change)
// ============================================================

extern "C" JNIEXPORT void JNICALL
Java_app_revanced_extension_tiktok_settings_Settings_nativeReloadSettings(JNIEnv* env, jclass clazz) {
    LOGI("Reloading settings from SharedPreferences...");

    jobject context = Utils::getApplicationContext(env);
    if (context != nullptr) {
        Settings::load(env, context);
        LOGI("Settings reloaded successfully");
    } else {
        LOGE("Cannot reload settings: no context available");
    }
}

// Note: special_clinit methods are implemented in special_clinit.cpp

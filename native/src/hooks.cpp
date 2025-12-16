/**
 * Hooks - Native Method Registration
 *
 * Registers native methods for various classes.
 * Replaces the original tigrik dynamic registration system.
 *
 * NO CLOUD CONTROL - Skips banner/kill switch registration.
 */

#include "tikmod.h"
#include <map>

// ============================================================
// Forward declarations for native methods (defined in tikmod.cpp)
// ============================================================
extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getVideoWatermark(JNIEnv*, jclass);

JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getPicWatermark(JNIEnv*, jclass);

JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getCommentPicWatermark(JNIEnv*, jclass);

JNIEXPORT void JNICALL
Java_com_tiktok_plugin_client_feed_FeedItemsProcess_processFeedItems(JNIEnv*, jclass, jobject);

JNIEXPORT jboolean JNICALL
Java_com_tiktok_plugin_client_ui_RememberClearDisplay_getClearDisplayState(JNIEnv*, jclass);

JNIEXPORT void JNICALL
Java_com_tiktok_plugin_client_ui_RememberClearDisplay_setClearDisplayState(JNIEnv*, jclass, jboolean);

JNIEXPORT jfloat JNICALL
Java_com_tiktok_plugin_client_ui_RememberPlaybackSpeed_getPlaybackSpeed(JNIEnv*, jclass);

JNIEXPORT void JNICALL
Java_com_tiktok_plugin_client_ui_RememberPlaybackSpeed_setPlaybackSpeed(JNIEnv*, jclass, jfloat);

}

namespace Hooks {

// Map of class index to registration status
static std::map<int, bool> registeredClasses;

// ============================================================
// Class Index Reference (from Ghidra analysis):
// ============================================================
// 0x00 (0)   - tigrik0.hidden.Hidden0
// 0x40 (64)  - me.tigrik.f.a (main settings class)
// 46         - Settings/Config system
// 47         - FeedItemsProcess
// 48         - RememberClearDisplay
// 49         - RememberPlaybackSpeed
// 50-52      - CrashActivity (we can skip or simplify)
// 53-54      - KillAppReceiver (SKIP - cloud control)
// 55         - me.tigrik.a.a
// 56-62      - UI dialogs
// 63-73      - Banner system (SKIP - cloud control)
// ============================================================

void initializeHooks(JNIEnv* env) {
    LOGI("Initializing hooks (NO CLOUD CONTROL)");

    // Load settings when context becomes available
    jobject context = Utils::getApplicationContext(env);
    if (context != nullptr) {
        Settings::load(env, context);
    }

    LOGI("Hooks initialized");
}

bool registerNatives(JNIEnv* env, int classIndex, jclass targetClass) {
    if (targetClass == nullptr) {
        LOGE("registerNatives called with null class for index %d", classIndex);
        return false;
    }

    // Check if already registered
    if (registeredClasses.count(classIndex) > 0 && registeredClasses[classIndex]) {
        LOGD("Class index %d already registered", classIndex);
        return true;
    }

    LOGD("Registering natives for class index: %d (0x%02X)", classIndex, classIndex);

    // Handle based on class index
    switch (classIndex) {
        // ============================================================
        // tigrik0.hidden.Hidden0 - Bootstrap class
        // ============================================================
        case 0: {
            LOGD("Registering Hidden0 natives (bootstrap)");
            break;
        }

        // ============================================================
        // me.tigrik.f.a - Main settings class (index 64 = 0x40)
        // ============================================================
        case 64: {
            LOGD("Registering main settings class natives");

            JNINativeMethod watermarkMethods[] = {
                {"getVideoWatermark", "()Z",
                    (void*)Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getVideoWatermark},
                {"getPicWatermark", "()Z",
                    (void*)Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getPicWatermark},
                {"getCommentPicWatermark", "()Z",
                    (void*)Java_com_tiktok_plugin_client_ClientSettings_00024Watermark_getCommentPicWatermark},
            };

            if (env->RegisterNatives(targetClass, watermarkMethods, 3) != JNI_OK) {
                env->ExceptionClear();
                LOGD("Watermark methods registration failed (expected if signatures differ)");
            }
            break;
        }

        // ============================================================
        // Settings/Config system (index 46)
        // ============================================================
        case 46: {
            LOGD("Registering config system");
            break;
        }

        // ============================================================
        // FeedItemsProcess (index 47)
        // ============================================================
        case 47: {
            LOGD("Registering FeedItemsProcess");

            JNINativeMethod feedMethods[] = {
                {"processFeedItems", "(Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;)V",
                    (void*)Java_com_tiktok_plugin_client_feed_FeedItemsProcess_processFeedItems},
            };

            if (env->RegisterNatives(targetClass, feedMethods, 1) != JNI_OK) {
                env->ExceptionClear();
                LOGD("FeedItemsProcess registration failed");
            }
            break;
        }

        // ============================================================
        // RememberClearDisplay (index 48)
        // ============================================================
        case 48: {
            LOGD("Registering RememberClearDisplay");

            JNINativeMethod clearDisplayMethods[] = {
                {"getClearDisplayState", "()Z",
                    (void*)Java_com_tiktok_plugin_client_ui_RememberClearDisplay_getClearDisplayState},
                {"setClearDisplayState", "(Z)V",
                    (void*)Java_com_tiktok_plugin_client_ui_RememberClearDisplay_setClearDisplayState},
            };

            if (env->RegisterNatives(targetClass, clearDisplayMethods, 2) != JNI_OK) {
                env->ExceptionClear();
            }
            break;
        }

        // ============================================================
        // RememberPlaybackSpeed (index 49)
        // ============================================================
        case 49: {
            LOGD("Registering RememberPlaybackSpeed");

            JNINativeMethod speedMethods[] = {
                {"getPlaybackSpeed", "()F",
                    (void*)Java_com_tiktok_plugin_client_ui_RememberPlaybackSpeed_getPlaybackSpeed},
                {"setPlaybackSpeed", "(F)V",
                    (void*)Java_com_tiktok_plugin_client_ui_RememberPlaybackSpeed_setPlaybackSpeed},
            };

            if (env->RegisterNatives(targetClass, speedMethods, 2) != JNI_OK) {
                env->ExceptionClear();
            }
            break;
        }

        // ============================================================
        // CrashActivity (indices 50-52) - Simplified
        // ============================================================
        case 50:
        case 51:
        case 52: {
            LOGD("Skipping CrashActivity natives (not needed without cloud)");
            break;
        }

        // ============================================================
        // KillAppReceiver (indices 53-54) - DISABLED
        // ============================================================
        case 53:
        case 54: {
            LOGI("SKIPPING KillAppReceiver - No remote kill switch!");
            break;
        }

        // ============================================================
        // me.tigrik.a.a utilities (index 55)
        // ============================================================
        case 55: {
            LOGD("Registering utility class");
            break;
        }

        // ============================================================
        // UI Dialog classes (indices 56-62)
        // ============================================================
        case 56:
        case 57:
        case 58:
        case 59:
        case 60:
        case 61:
        case 62: {
            LOGD("Registering UI dialog class %d", classIndex);
            break;
        }

        // ============================================================
        // Banner system (indices 63, 65-73) - DISABLED
        // Note: 64 is handled above as settings class
        // ============================================================
        case 63:
        case 65:
        case 66:
        case 67:
        case 68:
        case 69:
        case 70:
        case 71:
        case 72:
        case 73: {
            LOGI("SKIPPING Banner system registration (index %d) - No cloud banners!", classIndex);
            break;
        }

        default: {
            LOGD("Unknown class index %d - skipping", classIndex);
            break;
        }
    }

    registeredClasses[classIndex] = true;
    return true;
}

} // namespace Hooks

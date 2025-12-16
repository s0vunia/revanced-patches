/**
 * Special Clinit Implementations
 *
 * This file implements all 74 special_clinit_XX_YY native methods from Hidden0.
 * These methods are called during class loading to register native implementations.
 *
 * Cloud control indices that we intentionally skip:
 * - 53-54: KillAppReceiver (kill switch)
 * - 63-73: Banner system (cloud ads)
 */

#include "tikmod.h"

// Forward declaration
extern "C" void handleSpecialClinit(JNIEnv* env, int index, jclass targetClass);

// Macro to generate special_clinit implementations
#define SPECIAL_CLINIT(idx, suffix) \
extern "C" JNIEXPORT void JNICALL \
Java_tigrik0_hidden_Hidden0_special_1clinit_1##idx##_1##suffix(JNIEnv* env, jclass clazz, jclass targetClass) { \
    handleSpecialClinit(env, idx, targetClass); \
}

// Generate all 74 special_clinit methods
// Format: special_clinit_INDEX_SUFFIX

SPECIAL_CLINIT(0, 10)
SPECIAL_CLINIT(1, 620)
SPECIAL_CLINIT(2, 30)
SPECIAL_CLINIT(3, 60)
SPECIAL_CLINIT(4, 10)
SPECIAL_CLINIT(5, 10)
SPECIAL_CLINIT(6, 50)
SPECIAL_CLINIT(7, 20)
SPECIAL_CLINIT(8, 10)
SPECIAL_CLINIT(9, 10)
SPECIAL_CLINIT(10, 10)
SPECIAL_CLINIT(11, 10)
SPECIAL_CLINIT(12, 10)
SPECIAL_CLINIT(13, 20)
SPECIAL_CLINIT(14, 20)
SPECIAL_CLINIT(15, 10)
SPECIAL_CLINIT(16, 10)
SPECIAL_CLINIT(17, 10)
SPECIAL_CLINIT(18, 10)
SPECIAL_CLINIT(19, 10)
SPECIAL_CLINIT(20, 10)
SPECIAL_CLINIT(21, 10)
SPECIAL_CLINIT(22, 10)
SPECIAL_CLINIT(23, 10)
SPECIAL_CLINIT(24, 10)
SPECIAL_CLINIT(25, 20)
SPECIAL_CLINIT(26, 10)
SPECIAL_CLINIT(27, 10)
SPECIAL_CLINIT(28, 20)
SPECIAL_CLINIT(29, 20)
SPECIAL_CLINIT(30, 10)
SPECIAL_CLINIT(31, 10)
SPECIAL_CLINIT(32, 10)
SPECIAL_CLINIT(33, 10)
SPECIAL_CLINIT(34, 10)
SPECIAL_CLINIT(35, 70)
SPECIAL_CLINIT(36, 10)
SPECIAL_CLINIT(37, 30)
SPECIAL_CLINIT(38, 10)
SPECIAL_CLINIT(39, 10)
SPECIAL_CLINIT(40, 10)
SPECIAL_CLINIT(41, 20)
SPECIAL_CLINIT(42, 20)
SPECIAL_CLINIT(43, 60)
SPECIAL_CLINIT(44, 10)
SPECIAL_CLINIT(45, 30)
SPECIAL_CLINIT(46, 80)  // Settings/Config system
SPECIAL_CLINIT(47, 20)  // FeedItemsProcess
SPECIAL_CLINIT(48, 80)  // RememberClearDisplay
SPECIAL_CLINIT(49, 20)  // RememberPlaybackSpeed
SPECIAL_CLINIT(50, 20)  // CrashActivity
SPECIAL_CLINIT(51, 20)  // CrashActivity
SPECIAL_CLINIT(52, 20)  // CrashActivity
SPECIAL_CLINIT(53, 20)  // KillAppReceiver - CLOUD CONTROL (skip)
SPECIAL_CLINIT(54, 20)  // KillAppReceiver - CLOUD CONTROL (skip)
SPECIAL_CLINIT(55, 30)  // me.tigrik.a.a
SPECIAL_CLINIT(56, 00)  // UI dialog
SPECIAL_CLINIT(57, 30)  // UI dialog
SPECIAL_CLINIT(58, 20)  // UI dialog
SPECIAL_CLINIT(59, 20)  // UI dialog
SPECIAL_CLINIT(60, 20)  // UI dialog
SPECIAL_CLINIT(61, 30)  // UI dialog
SPECIAL_CLINIT(62, 00)  // UI dialog
SPECIAL_CLINIT(63, 00)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(64, 00)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(65, 20)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(66, 20)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(67, 40)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(68, 20)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(69, 20)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(70, 20)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(71, 20)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(72, 20)  // Banner system - CLOUD CONTROL (skip)
SPECIAL_CLINIT(73, 20)  // Banner system - CLOUD CONTROL (skip)

/**
 * Main handler for all special_clinit calls.
 * Decides what native methods to register based on the class index.
 */
extern "C" void handleSpecialClinit(JNIEnv* env, int index, jclass targetClass) {
    LOGD("special_clinit_%d called", index);

    // Skip cloud control indices
    if (index >= 53 && index <= 54) {
        LOGI("SKIPPING KillAppReceiver registration (index %d) - No remote kill switch!", index);
        return;
    }

    if (index >= 63 && index <= 73) {
        LOGI("SKIPPING Banner system registration (index %d) - No cloud banners!", index);
        return;
    }

    // For other indices, delegate to the Hooks system
    Hooks::registerNatives(env, index, targetClass);
}

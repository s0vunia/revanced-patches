/**
 * Watermark Control
 *
 * Functions to control watermark display on videos and images.
 */

#include "tikmod.h"

namespace Watermark {

bool getVideoWatermark() {
    // Return true if watermark should be shown (false = removed)
    return !g_settings.removeVideoWatermark;
}

bool getPicWatermark() {
    return !g_settings.removePicWatermark;
}

bool getCommentPicWatermark() {
    return !g_settings.removeCommentPicWatermark;
}

void setVideoWatermark(bool enabled) {
    g_settings.removeVideoWatermark = !enabled;

    JNIEnv* env = Utils::getEnv();
    if (env != nullptr && g_context != nullptr) {
        Settings::save(env, g_context);
    }
}

void setPicWatermark(bool enabled) {
    g_settings.removePicWatermark = !enabled;

    JNIEnv* env = Utils::getEnv();
    if (env != nullptr && g_context != nullptr) {
        Settings::save(env, g_context);
    }
}

void setCommentPicWatermark(bool enabled) {
    g_settings.removeCommentPicWatermark = !enabled;

    JNIEnv* env = Utils::getEnv();
    if (env != nullptr && g_context != nullptr) {
        Settings::save(env, g_context);
    }
}

} // namespace Watermark

namespace Download {

bool isDownloadEnabled() {
    return g_settings.enableDownload;
}

bool isDownloadViaTelegram() {
    return g_settings.downloadViaTelegram;
}

bool isSingleImageModeDownload() {
    return g_settings.singleImageModeDownload;
}

void setDownloadEnabled(bool enabled) {
    g_settings.enableDownload = enabled;

    JNIEnv* env = Utils::getEnv();
    if (env != nullptr && g_context != nullptr) {
        Settings::save(env, g_context);
    }
}

void setDownloadViaTelegram(bool enabled) {
    g_settings.downloadViaTelegram = enabled;

    JNIEnv* env = Utils::getEnv();
    if (env != nullptr && g_context != nullptr) {
        Settings::save(env, g_context);
    }
}

void setSingleImageModeDownload(bool enabled) {
    g_settings.singleImageModeDownload = enabled;

    JNIEnv* env = Utils::getEnv();
    if (env != nullptr && g_context != nullptr) {
        Settings::save(env, g_context);
    }
}

} // namespace Download

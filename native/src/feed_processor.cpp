/**
 * Feed Processor - Ad Blocking and Content Filtering
 *
 * Processes TikTok feed items to:
 * - Remove ads
 * - Filter live streams
 * - Filter image posts
 * - Filter by view/like counts
 * - Remove promotional content
 */

#include "tikmod.h"
#include <vector>

namespace FeedProcessor {

// Cached class and method IDs for performance
static jclass awemeClass = nullptr;
static jclass videoClass = nullptr;
static jclass statisticsClass = nullptr;
static jclass listClass = nullptr;
static jclass iteratorClass = nullptr;

static jmethodID listIterator = nullptr;
static jmethodID iteratorHasNext = nullptr;
static jmethodID iteratorNext = nullptr;
static jmethodID listRemove = nullptr;
static jmethodID listSize = nullptr;
static jmethodID listGet = nullptr;

// Aweme methods
static jmethodID getVideo = nullptr;
static jmethodID getStatistics = nullptr;
static jmethodID isAd = nullptr;
static jmethodID isLive = nullptr;
static jmethodID isPhotoMode = nullptr;

// Video methods
static jmethodID getDuration = nullptr;

// Statistics methods
static jmethodID getDiggCount = nullptr;
static jmethodID getPlayCount = nullptr;

static bool initialized = false;

void initClassRefs(JNIEnv* env) {
    if (initialized) return;

    // List/Iterator classes
    jclass localListClass = env->FindClass("java/util/List");
    if (localListClass != nullptr) {
        listClass = (jclass)env->NewGlobalRef(localListClass);
        listIterator = env->GetMethodID(listClass, "iterator", "()Ljava/util/Iterator;");
        listRemove = env->GetMethodID(listClass, "remove", "(Ljava/lang/Object;)Z");
        listSize = env->GetMethodID(listClass, "size", "()I");
        listGet = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
        env->DeleteLocalRef(localListClass);
    }

    jclass localIterClass = env->FindClass("java/util/Iterator");
    if (localIterClass != nullptr) {
        iteratorClass = (jclass)env->NewGlobalRef(localIterClass);
        iteratorHasNext = env->GetMethodID(iteratorClass, "hasNext", "()Z");
        iteratorNext = env->GetMethodID(iteratorClass, "next", "()Ljava/lang/Object;");
        env->DeleteLocalRef(localIterClass);
    }

    // Aweme class
    jclass localAwemeClass = env->FindClass("com/ss/android/ugc/aweme/feed/model/Aweme");
    if (localAwemeClass != nullptr) {
        awemeClass = (jclass)env->NewGlobalRef(localAwemeClass);
        getVideo = env->GetMethodID(awemeClass, "getVideo", "()Lcom/ss/android/ugc/aweme/feed/model/Video;");
        getStatistics = env->GetMethodID(awemeClass, "getStatistics", "()Lcom/ss/android/ugc/aweme/feed/model/AwemeStatistics;");

        // Try to find ad detection method (may have different names)
        isAd = env->GetMethodID(awemeClass, "isAd", "()Z");
        if (isAd == nullptr) {
            env->ExceptionClear();
            isAd = env->GetMethodID(awemeClass, "getIsAd", "()Z");
        }
        if (isAd == nullptr) env->ExceptionClear();

        isLive = env->GetMethodID(awemeClass, "isLive", "()Z");
        if (isLive == nullptr) env->ExceptionClear();

        isPhotoMode = env->GetMethodID(awemeClass, "isPhotoMode", "()Z");
        if (isPhotoMode == nullptr) {
            env->ExceptionClear();
            isPhotoMode = env->GetMethodID(awemeClass, "isImage", "()Z");
        }
        if (isPhotoMode == nullptr) env->ExceptionClear();

        env->DeleteLocalRef(localAwemeClass);
    }

    // Video class
    jclass localVideoClass = env->FindClass("com/ss/android/ugc/aweme/feed/model/Video");
    if (localVideoClass != nullptr) {
        videoClass = (jclass)env->NewGlobalRef(localVideoClass);
        getDuration = env->GetMethodID(videoClass, "getDuration", "()J");
        if (getDuration == nullptr) env->ExceptionClear();
        env->DeleteLocalRef(localVideoClass);
    }

    // Statistics class
    jclass localStatsClass = env->FindClass("com/ss/android/ugc/aweme/feed/model/AwemeStatistics");
    if (localStatsClass != nullptr) {
        statisticsClass = (jclass)env->NewGlobalRef(localStatsClass);
        getDiggCount = env->GetMethodID(statisticsClass, "getDiggCount", "()J");
        if (getDiggCount == nullptr) env->ExceptionClear();
        getPlayCount = env->GetMethodID(statisticsClass, "getPlayCount", "()J");
        if (getPlayCount == nullptr) env->ExceptionClear();
        env->DeleteLocalRef(localStatsClass);
    }

    initialized = true;
    LOGD("FeedProcessor class refs initialized");
}

bool isAdContent(JNIEnv* env, jobject aweme) {
    if (aweme == nullptr || isAd == nullptr) return false;

    return env->CallBooleanMethod(aweme, isAd) == JNI_TRUE;
}

bool isLiveStream(JNIEnv* env, jobject aweme) {
    if (aweme == nullptr || isLive == nullptr) return false;

    return env->CallBooleanMethod(aweme, isLive) == JNI_TRUE;
}

bool isImagePost(JNIEnv* env, jobject aweme) {
    if (aweme == nullptr || isPhotoMode == nullptr) return false;

    return env->CallBooleanMethod(aweme, isPhotoMode) == JNI_TRUE;
}

bool isLongPost(JNIEnv* env, jobject aweme) {
    if (aweme == nullptr || getVideo == nullptr || getDuration == nullptr) return false;

    jobject video = env->CallObjectMethod(aweme, getVideo);
    if (video == nullptr) return false;

    jlong duration = env->CallLongMethod(video, getDuration);
    env->DeleteLocalRef(video);

    // Consider posts longer than 3 minutes (180000ms) as "long"
    return duration > 180000;
}

bool hasPromotionalMusic(JNIEnv* env, jobject aweme) {
    // This would require checking music/sound metadata
    // For now, return false (don't filter)
    return false;
}

bool passesViewsFilter(JNIEnv* env, jobject aweme) {
    if (!g_settings.filterByViews || g_settings.minViewsCount <= 0) return true;
    if (aweme == nullptr || getStatistics == nullptr || getPlayCount == nullptr) return true;

    jobject stats = env->CallObjectMethod(aweme, getStatistics);
    if (stats == nullptr) return true;

    jlong playCount = env->CallLongMethod(stats, getPlayCount);
    env->DeleteLocalRef(stats);

    return playCount >= g_settings.minViewsCount;
}

bool passesLikesFilter(JNIEnv* env, jobject aweme) {
    if (!g_settings.filterByLikes || g_settings.minLikesCount <= 0) return true;
    if (aweme == nullptr || getStatistics == nullptr || getDiggCount == nullptr) return true;

    jobject stats = env->CallObjectMethod(aweme, getStatistics);
    if (stats == nullptr) return true;

    jlong diggCount = env->CallLongMethod(stats, getDiggCount);
    env->DeleteLocalRef(stats);

    return diggCount >= g_settings.minLikesCount;
}

bool shouldFilterAweme(JNIEnv* env, jobject aweme) {
    if (aweme == nullptr) return false;

    // Check ad content
    if (g_settings.hideAds && isAdContent(env, aweme)) {
        LOGD("Filtering ad content");
        return true;
    }

    // Check live streams
    if (g_settings.hideLiveStreams && isLiveStream(env, aweme)) {
        LOGD("Filtering live stream");
        return true;
    }

    // Check image posts
    if (g_settings.hideImagePosts && isImagePost(env, aweme)) {
        LOGD("Filtering image post");
        return true;
    }

    // Check long posts
    if (g_settings.hideLongPosts && isLongPost(env, aweme)) {
        LOGD("Filtering long post");
        return true;
    }

    // Check promotional music
    if (g_settings.hidePromotionalMusic && hasPromotionalMusic(env, aweme)) {
        LOGD("Filtering promotional music content");
        return true;
    }

    // Check views filter
    if (!passesViewsFilter(env, aweme)) {
        LOGD("Filtering by views count");
        return true;
    }

    // Check likes filter
    if (!passesLikesFilter(env, aweme)) {
        LOGD("Filtering by likes count");
        return true;
    }

    return false;
}

void processAwemeList(JNIEnv* env, jobject feedItemList) {
    initClassRefs(env);

    if (feedItemList == nullptr || listClass == nullptr) return;

    // Get the aweme list from FeedItemList
    jclass feedItemListClass = env->GetObjectClass(feedItemList);
    jmethodID getAwemeList = env->GetMethodID(feedItemListClass, "getAwemeList", "()Ljava/util/List;");
    if (getAwemeList == nullptr) {
        env->ExceptionClear();
        // Try alternative method name
        getAwemeList = env->GetMethodID(feedItemListClass, "getAwemes", "()Ljava/util/List;");
    }

    if (getAwemeList == nullptr) {
        env->ExceptionClear();
        LOGD("Could not find aweme list getter");
        return;
    }

    jobject awemeList = env->CallObjectMethod(feedItemList, getAwemeList);
    if (awemeList == nullptr) return;

    // Iterate and collect items to remove
    std::vector<jobject> toRemove;

    jint size = env->CallIntMethod(awemeList, listSize);
    for (jint i = 0; i < size; i++) {
        jobject aweme = env->CallObjectMethod(awemeList, listGet, i);
        if (aweme != nullptr && shouldFilterAweme(env, aweme)) {
            toRemove.push_back(env->NewGlobalRef(aweme));
        }
        if (aweme != nullptr) {
            env->DeleteLocalRef(aweme);
        }
    }

    // Remove filtered items
    for (jobject item : toRemove) {
        env->CallBooleanMethod(awemeList, listRemove, item);
        env->DeleteGlobalRef(item);
    }

    if (!toRemove.empty()) {
        LOGI("Filtered %zu items from feed", toRemove.size());
    }

    env->DeleteLocalRef(awemeList);
}

void processDiscoverBanners(JNIEnv* env, jobject bannerList) {
    if (bannerList == nullptr || !g_settings.removeDiscoverAds) return;

    // Get banner list
    jclass bannerListClass = env->GetObjectClass(bannerList);
    jmethodID getBanners = env->GetMethodID(bannerListClass, "getBanners", "()Ljava/util/List;");
    if (getBanners == nullptr) {
        env->ExceptionClear();
        getBanners = env->GetMethodID(bannerListClass, "getBannerList", "()Ljava/util/List;");
    }

    if (getBanners == nullptr) {
        env->ExceptionClear();
        return;
    }

    jobject banners = env->CallObjectMethod(bannerList, getBanners);
    if (banners == nullptr) return;

    // Clear all banners (they're ads)
    jclass listClassLocal = env->GetObjectClass(banners);
    jmethodID clear = env->GetMethodID(listClassLocal, "clear", "()V");

    if (clear != nullptr) {
        env->CallVoidMethod(banners, clear);
        LOGD("Cleared discover banner ads");
    }

    env->DeleteLocalRef(banners);
}

void processDiscoverTopics(JNIEnv* env, jobject topicList) {
    // Topics can be processed similarly if needed
    // For now, we don't filter topics as they're usually not ads
}

} // namespace FeedProcessor

package app.revanced.patches.tiktok.feedfilter

import app.revanced.patcher.fingerprint
<<<<<<< HEAD
import com.android.tools.smali.dexlib2.AccessFlags

internal val feedApiServiceLIZFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.endsWith("/FeedApiService;") && method.name == "fetchFeedList"
    }
}

internal val followFeedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;")
    strings(
        "userAvatarShrink",
        "adUserAgent",
        "localAwemes",
        "redDotState",
    )
}
=======

/**
 * Fingerprint to find the method that processes feed items in TikTok.
 * This method is called when feed data is loaded and contains the list of items to display.
 */
internal val feedItemListFingerprint = fingerprint {
    returns("V")
    strings("FeedItemList", "items")
    custom { method, _ ->
        method.definingClass.endsWith("FeedItemList;") ||
        method.name.contains("onFeedLoaded") ||
        method.name.contains("processFeed")
    }
}

/**
 * Alternative fingerprint targeting the RecyclerView adapter that displays feed items.
 * TikTok uses different class names across versions, so we use multiple fingerprints.
 */
internal val feedRecyclerFingerprint = fingerprint {
    returns("V")
    parameters("Ljava/util/List;")
    strings("aweme_list", "item_list")
    custom { method, classDef ->
        classDef.type.contains("Feed") &&
        (method.name == "setItems" || method.name == "updateItems" || method.name.contains("List"))
    }
}

/**
 * Fingerprint for the Pangle/Bytedance ad SDK initialization.
 * Blocking this prevents ads from being loaded at all.
 */
internal val pangleAdInitFingerprint = fingerprint {
    returns("V")
    strings("TTAdSdk", "init", "appId")
    custom { method, classDef ->
        classDef.type.contains("pangle") ||
        classDef.type.contains("TTAdSdk") ||
        classDef.type.contains("bytedance/sdk/openadsdk")
    }
}

/**
 * Fingerprint for ad insertion point in the feed.
 * TikTok inserts ads at specific positions in the feed list.
 */
internal val adInsertFingerprint = fingerprint {
    returns("V")
    strings("ad_", "is_ad", "ad_type", "promoted")
    custom { method, _ ->
        method.name.contains("insert") || method.name.contains("add")
    }
}

/**
 * Fingerprint for the ad model class check.
 * Used to identify which items in the feed are ads.
 */
internal val isAdCheckFingerprint = fingerprint {
    returns("Z")
    custom { method, classDef ->
        (method.name == "isAd" || method.name == "isPromoted" || method.name == "isSponsored") &&
        (classDef.type.contains("Aweme") || classDef.type.contains("FeedItem"))
    }
}
>>>>>>> 2172b6f60 (feat: TikTok ReVanced standalone mod implementation)

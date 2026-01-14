package app.revanced.patches.tiktok.banner

import app.revanced.patcher.fingerprint

/**
 * Fingerprint for TikTok's MainActivity.onCreate() method.
 * This is the main entry point where we inject the banner display call.
 */
internal val mainActivityOnCreateFingerprint = fingerprint {
    returns("V")
    parameters("Landroid/os/Bundle;")
    custom { method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/main/MainActivity;" &&
        method.name == "onCreate"
    }
}

/**
 * Alternative fingerprint for TikTok Lite's MainActivity.
 */
internal val mainActivityLiteOnCreateFingerprint = fingerprint {
    returns("V")
    parameters("Landroid/os/Bundle;")
    custom { method, classDef ->
        classDef.type.contains("MainActivity") &&
        classDef.type.contains("aweme") &&
        method.name == "onCreate"
    }
}

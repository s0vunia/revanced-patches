package app.revanced.patches.tiktok.banner

import app.revanced.patcher.fingerprint

/**
 * Fingerprint for TikTok's AwemeHostApplication.onCreate() method.
 *
 * NOTE: MainActivity.onCreate() is NATIVE in TikTok 43.x, so we can't inject there.
 * Using AwemeHostApplication.onCreate() which is the Application class.
 */
internal val awemeHostApplicationOnCreateFingerprint = fingerprint {
    returns("V")
    parameters()
    custom { method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/app/host/AwemeHostApplication;" &&
        method.name == "onCreate"
    }
}

/**
 * Alternative fingerprint - JatoInitTask.run() which has Context parameter.
 * Called during app startup.
 */
internal val jatoInitTaskFingerprint = fingerprint {
    returns("V")
    parameters("Landroid/content/Context;")
    custom { method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/legoImp/task/JatoInitTask;" &&
        method.name == "run"
    }
}

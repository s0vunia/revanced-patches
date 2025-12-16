package app.revanced.patches.tiktok.nativelib

import app.revanced.patcher.fingerprint

/**
 * Fingerprint for the main tigrik0/tigrik class.
 * This class loads the native library via System.loadLibrary("tigrik").
 */
internal val tigrikClassFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type == "Ltigrik0/tigrik;" &&
        method.name == "<clinit>"
    }
}

/**
 * Fingerprint for the Hidden0 class that contains special_clinit methods.
 * These methods register native implementations for various features.
 */
internal val hidden0ClassFingerprint = fingerprint {
    custom { _, classDef ->
        classDef.type == "Ltigrik0/hidden/Hidden0;"
    }
}

/**
 * Fingerprint for the KillAppReceiver's onReceive method.
 * This is the kill switch that allows remote termination.
 */
internal val killAppReceiverFingerprint = fingerprint {
    returns("V")
    parameters("Landroid/content/Context;", "Landroid/content/Intent;")
    custom { method, classDef ->
        classDef.type == "Lme/tigrik/KillAppReceiver;" &&
        method.name == "onReceive"
    }
}

/**
 * Fingerprint for the banner fetching initialization.
 * Located in me/tigrik/f/a class (main banner class).
 */
internal val bannerFetchFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type == "Lme/tigrik/f/a;" &&
        (method.name == "a" || method.name == "b") &&
        method.parameters.any { it.type.contains("Context") }
    }
}

/**
 * Fingerprint for banner display methods in me/tigrik/f/e-j classes.
 */
internal val bannerDisplayFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type.startsWith("Lme/tigrik/f/") &&
        method.returnType == "V" &&
        method.parameters.any { it.type.contains("Context") || it.type.contains("View") }
    }
}

/**
 * Fingerprint for the registerNativesForClass method.
 * This is the main entry point for native method registration.
 */
internal val registerNativesFingerprint = fingerprint {
    returns("V")
    parameters("I", "Ljava/lang/Class;")
    custom { method, classDef ->
        classDef.type == "Ltigrik0/tigrik;" &&
        method.name == "registerNativesForClass"
    }
}

/**
 * Fingerprint for CrashActivity - we can optionally disable this.
 */
internal val crashActivityFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type == "Lme/tigrik/CrashActivity;" &&
        method.name == "onCreate"
    }
}

/**
 * Fingerprint for settings class me/tigrik/f/a.
 * This stores and retrieves mod settings.
 */
internal val settingsClassFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type == "Lme/tigrik/f/a;" &&
        (method.name == "a" && method.returnType == "Ljava/lang/String;")
    }
}

/**
 * Fingerprint for dialog classes in me/tigrik/b/.
 * These show UI dialogs for various mod features.
 */
internal val dialogClassFingerprint = fingerprint {
    custom { _, classDef ->
        classDef.type.startsWith("Lme/tigrik/b/") &&
        classDef.type.length == "Lme/tigrik/b/a;".length
    }
}

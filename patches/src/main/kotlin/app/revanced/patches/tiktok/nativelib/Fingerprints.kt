package app.revanced.patches.tiktok.nativelib

import app.revanced.patcher.fingerprint

/**
 * Fingerprint for the main native library loader class.
 * This class loads the native library via System.loadLibrary().
 * Note: The library name cannot be changed as TikTok expects "libtigrik.so".
 */
internal val nativeLoaderClassFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type == "Ltigrik0/tigrik;" &&
        method.name == "<clinit>"
    }
}

/**
 * Fingerprint for the Hidden0 class that contains special_clinit methods.
 * These methods register native implementations for cloud control features.
 * Indices 53-54: Remote kill switch
 * Indices 63-73: Remote banner system
 */
internal val nativeRegistrationClassFingerprint = fingerprint {
    custom { _, classDef ->
        classDef.type == "Ltigrik0/hidden/Hidden0;"
    }
}

/**
 * Fingerprint for the remote kill switch receiver.
 * ByteDance uses this to remotely terminate modified apps.
 * Our mod disables this to prevent remote termination.
 */
internal val remoteKillSwitchFingerprint = fingerprint {
    returns("V")
    parameters("Landroid/content/Context;", "Landroid/content/Intent;")
    custom { method, classDef ->
        classDef.type == "Lme/tigrik/KillAppReceiver;" &&
        method.name == "onReceive"
    }
}

/**
 * Fingerprint for remote banner ad fetching.
 * This downloads and displays cloud-served advertisements.
 */
internal val remoteBannerFetchFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type == "Lme/tigrik/f/a;" &&
        (method.name == "a" || method.name == "b") &&
        method.parameters.any { it.type.contains("Context") }
    }
}

/**
 * Fingerprint for remote banner display methods.
 * These show cloud-fetched ads in the app UI.
 */
internal val remoteBannerDisplayFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type.startsWith("Lme/tigrik/f/") &&
        method.returnType == "V" &&
        method.parameters.any { it.type.contains("Context") || it.type.contains("View") }
    }
}

/**
 * Fingerprint for native method registration.
 * This registers cloud control functions at the native layer.
 */
internal val nativeMethodRegistrationFingerprint = fingerprint {
    returns("V")
    parameters("I", "Ljava/lang/Class;")
    custom { method, classDef ->
        classDef.type == "Ltigrik0/tigrik;" &&
        method.name == "registerNativesForClass"
    }
}

/**
 * Fingerprint for crash reporting activity.
 * Sends error telemetry to ByteDance servers.
 */
internal val crashReportingFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type == "Lme/tigrik/CrashActivity;" &&
        method.name == "onCreate"
    }
}

/**
 * Fingerprint for cloud settings storage.
 * Stores remote configuration from ByteDance servers.
 */
internal val cloudSettingsFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type == "Lme/tigrik/f/a;" &&
        (method.name == "a" && method.returnType == "Ljava/lang/String;")
    }
}

/**
 * Fingerprint for cloud dialog classes.
 * These show remote-triggered UI dialogs (e.g., forced updates).
 */
internal val cloudDialogFingerprint = fingerprint {
    custom { _, classDef ->
        classDef.type.startsWith("Lme/tigrik/b/") &&
        classDef.type.length == "Lme/tigrik/b/a;".length
    }
}

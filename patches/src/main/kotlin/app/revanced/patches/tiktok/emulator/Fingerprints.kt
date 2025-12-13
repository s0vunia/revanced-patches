package app.revanced.patches.tiktok.emulator

import app.revanced.patcher.fingerprint

/**
 * Fingerprint for Build class field access.
 * TikTok reads Build.HARDWARE, Build.MODEL, etc. to detect emulators.
 */
internal val buildHardwareFingerprint = fingerprint {
    custom { method, _ ->
        method.definingClass == "Landroid/os/Build;" &&
        method.name == "HARDWARE"
    }
}

/**
 * Fingerprint for generic emulator detection methods.
 * Matches methods that check for emulator indicators.
 */
internal val emulatorDetectionFingerprint = fingerprint {
    returns("Z")
    strings("goldfish", "sdk", "emulator", "generic")
    custom { method, _ ->
        method.name.lowercase().contains("emulator") ||
        method.name.lowercase().contains("isemu") ||
        method.name.lowercase().contains("virtual")
    }
}

/**
 * Fingerprint for methods that check device properties.
 * Often named isXXX or checkXXX.
 */
internal val deviceCheckFingerprint = fingerprint {
    returns("Z")
    custom { method, classDef ->
        (method.name.startsWith("is") || method.name.startsWith("check")) &&
        (classDef.type.contains("Device") || classDef.type.contains("Security") ||
         classDef.type.contains("Environment") || classDef.type.contains("Detect"))
    }
}

/**
 * Fingerprint for File.exists() checks on emulator-specific paths.
 */
internal val fileExistsCheckFingerprint = fingerprint {
    returns("Z")
    strings("/dev/qemu", "/dev/goldfish", "qemu_pipe", "qemud")
}

/**
 * Fingerprint for System.getProperty() calls checking ro.* properties.
 */
internal val systemPropertyCheckFingerprint = fingerprint {
    strings("ro.hardware", "ro.product", "ro.kernel.qemu", "ro.build")
    custom { method, _ ->
        method.name.contains("getProperty") || method.name.contains("getProp")
    }
}

/**
 * Fingerprint for TelephonyManager.getDeviceId() / getImei().
 * Emulators have default/null IMEI.
 */
internal val telephonyImeiFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type.contains("TelephonyManager") &&
        (method.name == "getDeviceId" || method.name == "getImei")
    }
}

/**
 * Fingerprint for TelephonyManager.getLine1Number().
 * Emulators have default phone number 15555215554.
 */
internal val telephonyPhoneNumberFingerprint = fingerprint {
    custom { method, classDef ->
        classDef.type.contains("TelephonyManager") &&
        method.name == "getLine1Number"
    }
}

/**
 * Fingerprint for sensor availability checks.
 * Emulators often lack accelerometer, gyroscope, etc.
 */
internal val sensorCheckFingerprint = fingerprint {
    returns("Z")
    strings("accelerometer", "gyroscope", "sensor")
    custom { method, classDef ->
        classDef.type.contains("Sensor") &&
        (method.name.contains("has") || method.name.contains("available"))
    }
}

/**
 * Fingerprint for ByteDance security/device check classes.
 * TikTok uses their own security SDK for detection.
 */
internal val bytedanceSecurityCheckFingerprint = fingerprint {
    returns("Z")
    custom { method, classDef ->
        (classDef.type.contains("com/bytedance") || classDef.type.contains("com/ss/android")) &&
        (classDef.type.contains("security") || classDef.type.contains("device") ||
         classDef.type.contains("risk") || classDef.type.contains("antifraud")) &&
        (method.name.startsWith("is") || method.name.startsWith("check") ||
         method.name.startsWith("detect") || method.name.startsWith("verify"))
    }
}

/**
 * Fingerprint for methods checking CPU architecture/ABI.
 * x86/x86_64 often indicates emulator.
 */
internal val cpuArchCheckFingerprint = fingerprint {
    strings("x86", "x86_64", "arm64-v8a", "armeabi")
    custom { method, _ ->
        method.name.lowercase().contains("cpu") ||
        method.name.lowercase().contains("arch") ||
        method.name.lowercase().contains("abi")
    }
}

/**
 * Fingerprint for AppsFlyerLib device checks.
 * Some apps use AppsFlyer which has its own emulator detection.
 */
internal val appsFlyerEmulatorCheckFingerprint = fingerprint {
    returns("Z")
    custom { method, classDef ->
        classDef.type.contains("appsflyer") &&
        method.name.lowercase().contains("emulator")
    }
}

/**
 * Fingerprint for root/magisk detection that may overlap with emulator detection.
 */
internal val rootDetectionFingerprint = fingerprint {
    returns("Z")
    strings("su", "/system/xbin/su", "/system/bin/su", "Superuser", "magisk")
}

/**
 * Fingerprint for battery status checks.
 * Emulators often show battery as always charging.
 */
internal val batteryCheckFingerprint = fingerprint {
    strings("android.intent.action.BATTERY_CHANGED", "plugged", "status")
    custom { method, _ ->
        method.name.lowercase().contains("battery") ||
        method.name.lowercase().contains("power")
    }
}

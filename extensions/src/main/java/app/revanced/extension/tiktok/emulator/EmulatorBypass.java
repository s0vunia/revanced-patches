package app.revanced.extension.tiktok.emulator;

import android.os.Build;
import android.util.Log;

import java.io.File;

import app.revanced.extension.tiktok.settings.Settings;

/**
 * Emulator Detection Bypass for TikTok.
 * Spoofs device properties to appear as a real device when running on an emulator.
 *
 * TikTok detects emulators via:
 * - Build properties (model, hardware, fingerprint, etc.)
 * - File system checks (qemu files, goldfish, etc.)
 * - Telephony info (IMEI, phone number)
 * - Sensor availability
 * - CPU architecture checks
 */
public final class EmulatorBypass {

    private static final String TAG = "EmulatorBypass";

    // Emulator detection strings
    private static final String[] EMULATOR_HARDWARE = {
        "goldfish", "ranchu", "vbox86", "nox", "ttVM_x86", "andy", "bluestacks"
    };

    private static final String[] EMULATOR_MODELS = {
        "sdk", "google_sdk", "Emulator", "Android SDK", "Droid4X", "TiantianVM",
        "Andy", "vbox86p", "nox", "BlueStacks"
    };

    private static final String[] EMULATOR_PRODUCTS = {
        "sdk", "google_sdk", "sdk_x86", "vbox86p", "emulator", "simulator",
        "sdk_gphone", "sdk_gphone64", "emulator64_x86_64"
    };

    private static final String[] EMULATOR_FILES = {
        "/dev/socket/qemud",
        "/dev/qemu_pipe",
        "/system/lib/libc_malloc_debug_qemu.so",
        "/sys/qemu_trace",
        "/system/bin/qemu-props",
        "/dev/goldfish_pipe",
        "/dev/vboxguest",
        "/dev/vboxuser",
        "/fstab.andy",
        "/fstab.nox",
        "/init.nox.rc",
        "/ueventd.nox.rc"
    };

    private EmulatorBypass() {
        // Static utility class
    }

    /**
     * Check if emulator bypass is enabled.
     */
    public static boolean isBypassEnabled() {
        return Settings.isEmulatorBypassEnabled();
    }

    // ==================== BUILD PROPERTY SPOOFS ====================

    /**
     * Spoof Build.HARDWARE to hide emulator.
     */
    public static String getHardware() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Spoofing Build.HARDWARE: qcom");
            return "qcom";  // Common Qualcomm hardware
        }
        return Build.HARDWARE;
    }

    /**
     * Spoof Build.MODEL to hide emulator.
     */
    public static String getModel() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Spoofing Build.MODEL: SM-G998B");
            return "SM-G998B";  // Samsung Galaxy S21 Ultra
        }
        return Build.MODEL;
    }

    /**
     * Spoof Build.MANUFACTURER to hide emulator.
     */
    public static String getManufacturer() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Spoofing Build.MANUFACTURER: samsung");
            return "samsung";
        }
        return Build.MANUFACTURER;
    }

    /**
     * Spoof Build.BRAND to hide emulator.
     */
    public static String getBrand() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Spoofing Build.BRAND: samsung");
            return "samsung";
        }
        return Build.BRAND;
    }

    /**
     * Spoof Build.DEVICE to hide emulator.
     */
    public static String getDevice() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Spoofing Build.DEVICE: p3s");
            return "p3s";  // Galaxy S21 Ultra device codename
        }
        return Build.DEVICE;
    }

    /**
     * Spoof Build.PRODUCT to hide emulator.
     */
    public static String getProduct() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Spoofing Build.PRODUCT: p3sxxx");
            return "p3sxxx";
        }
        return Build.PRODUCT;
    }

    /**
     * Spoof Build.BOARD to hide emulator.
     */
    public static String getBoard() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Spoofing Build.BOARD: exynos2100");
            return "exynos2100";
        }
        return Build.BOARD;
    }

    /**
     * Spoof Build.FINGERPRINT to hide emulator.
     */
    public static String getFingerprint() {
        if (isBypassEnabled()) {
            String fingerprint = "samsung/p3sxxx/p3s:13/TP1A.220624.014/G998BXXS7DWAA:user/release-keys";
            Log.d(TAG, "Spoofing Build.FINGERPRINT");
            return fingerprint;
        }
        return Build.FINGERPRINT;
    }

    // ==================== DETECTION CHECKS ====================

    /**
     * Check if a string contains emulator indicators.
     * Returns false (not emulator) if bypass is enabled.
     */
    public static boolean isEmulatorString(String value) {
        if (isBypassEnabled()) {
            return false;  // Never detect as emulator
        }
        if (value == null) return false;
        String lower = value.toLowerCase();
        for (String indicator : EMULATOR_HARDWARE) {
            if (lower.contains(indicator)) return true;
        }
        for (String indicator : EMULATOR_MODELS) {
            if (lower.contains(indicator.toLowerCase())) return true;
        }
        return false;
    }

    /**
     * Hook for File.exists() to hide emulator-specific files.
     */
    public static boolean fileExists(File file) {
        if (isBypassEnabled() && file != null) {
            String path = file.getAbsolutePath();
            for (String emulatorFile : EMULATOR_FILES) {
                if (path.equals(emulatorFile) || path.contains("qemu") || path.contains("goldfish")) {
                    Log.d(TAG, "Hiding emulator file: " + path);
                    return false;
                }
            }
        }
        return file != null && file.exists();
    }

    /**
     * Hook for System.getProperty() to spoof properties.
     */
    public static String getSystemProperty(String key, String defaultValue) {
        if (isBypassEnabled()) {
            switch (key) {
                case "ro.hardware":
                    return "qcom";
                case "ro.product.model":
                    return "SM-G998B";
                case "ro.product.brand":
                case "ro.product.manufacturer":
                    return "samsung";
                case "ro.product.device":
                    return "p3s";
                case "ro.product.name":
                    return "p3sxxx";
                case "ro.build.fingerprint":
                    return getFingerprint();
                case "ro.kernel.qemu":
                case "ro.kernel.android.qemud":
                    return "0";  // Not QEMU
                case "ro.bootimage.build.fingerprint":
                    return getFingerprint();
                case "gsm.version.baseband":
                    return "G998BXXS7DWAA";  // Real baseband version
                case "init.svc.qemud":
                case "init.svc.qemu-props":
                    return null;  // Service doesn't exist
            }
        }
        return defaultValue;
    }

    /**
     * Spoof IMEI to look like a real device.
     */
    public static String getImei() {
        if (isBypassEnabled()) {
            // Return a fake but valid-looking IMEI
            return "358673091234567";
        }
        return null;
    }

    /**
     * Spoof phone number (emulator default is 15555215554).
     */
    public static String getPhoneNumber() {
        if (isBypassEnabled()) {
            return null;  // Return null like many real devices
        }
        return null;
    }

    /**
     * Check if running on emulator - always return false when bypass enabled.
     */
    public static boolean isEmulator() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Bypassing emulator detection - returning false");
            return false;
        }
        // Actual emulator check
        return Build.FINGERPRINT.contains("generic")
            || Build.MODEL.contains("sdk")
            || Build.HARDWARE.contains("goldfish");
    }

    /**
     * Generic hook that returns false for any emulator detection method.
     */
    public static boolean bypassDetection() {
        if (isBypassEnabled()) {
            Log.d(TAG, "Bypassing emulator detection check");
            return false;
        }
        return true;  // Let original check proceed
    }

    /**
     * Hook for sensor availability - emulators often lack sensors.
     */
    public static boolean hasSensor() {
        if (isBypassEnabled()) {
            return true;  // Pretend all sensors exist
        }
        return false;
    }
}

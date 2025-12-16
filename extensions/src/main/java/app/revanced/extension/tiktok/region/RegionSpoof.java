package app.revanced.extension.tiktok.region;

import android.util.Log;

import app.revanced.extension.tiktok.settings.Settings;

/**
 * Extension for region spoofing in TikTok.
 *
 * Allows users to override detected region to access region-locked content.
 */
public final class RegionSpoof {
    private static final String TAG = "TikTokRegionSpoof";

    // Common region codes
    public static final String REGION_US = "US";
    public static final String REGION_UK = "GB";
    public static final String REGION_JP = "JP";
    public static final String REGION_KR = "KR";
    public static final String REGION_IN = "IN";
    public static final String REGION_BR = "BR";
    public static final String REGION_DE = "DE";
    public static final String REGION_FR = "FR";

    /**
     * Get the spoofed region code if region spoofing is enabled.
     * Returns null if spoofing is disabled (to use original region).
     */
    public static String getSpoofedRegion() {
        if (!Settings.isForceRegion()) {
            return null;
        }

        String region = Settings.getForcedRegion();
        if (region == null || region.isEmpty()) {
            return null;
        }

        // Normalize to uppercase
        region = region.toUpperCase().trim();

        // Validate region code (should be 2 letters)
        if (region.length() != 2) {
            Log.w(TAG, "Invalid region code: " + region + ", should be 2 letters (e.g., US, GB)");
            return null;
        }

        Log.d(TAG, "Spoofing region to: " + region);
        return region;
    }

    /**
     * Check if region spoofing is enabled.
     */
    public static boolean isRegionSpoofEnabled() {
        return Settings.isForceRegion();
    }

    /**
     * Called when region config is initialized.
     * Can be used to log or modify initialization behavior.
     */
    public static void onRegionConfigInit() {
        if (isRegionSpoofEnabled()) {
            String region = getSpoofedRegion();
            if (region != null) {
                Log.i(TAG, "Region config initialized with spoofed region: " + region);
            }
        }
    }

    /**
     * Get spoofed SIM country ISO code.
     */
    public static String getSpoofedSimCountryIso() {
        return getSpoofedRegion();
    }

    /**
     * Get spoofed network country ISO code.
     */
    public static String getSpoofedNetworkCountryIso() {
        return getSpoofedRegion();
    }

    /**
     * Validate a region code.
     */
    public static boolean isValidRegionCode(String code) {
        if (code == null || code.length() != 2) {
            return false;
        }

        // Check if all characters are letters
        for (char c : code.toCharArray()) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get list of common region codes for UI.
     */
    public static String[] getCommonRegionCodes() {
        return new String[]{
                "US - United States",
                "GB - United Kingdom",
                "JP - Japan",
                "KR - South Korea",
                "IN - India",
                "BR - Brazil",
                "DE - Germany",
                "FR - France",
                "CA - Canada",
                "AU - Australia",
                "MX - Mexico",
                "RU - Russia",
                "ID - Indonesia",
                "TH - Thailand",
                "VN - Vietnam",
                "PH - Philippines",
                "MY - Malaysia",
                "SG - Singapore",
                "TW - Taiwan",
                "HK - Hong Kong"
        };
    }
}

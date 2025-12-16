package app.revanced.patches.tiktok.region

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for getting SIM country code.
 * Target: TelephonyManager.getSimCountryIso()
 */
internal val getSimCountryIsoFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    custom { method, classDef ->
        method.name == "getSimCountryIso" &&
        classDef.type.endsWith("TelephonyManager;")
    }
}

/**
 * Fingerprint for getting network country code.
 * Target: TelephonyManager.getNetworkCountryIso()
 */
internal val getNetworkCountryIsoFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("Ljava/lang/String;")
    custom { method, classDef ->
        method.name == "getNetworkCountryIso" &&
        classDef.type.endsWith("TelephonyManager;")
    }
}

/**
 * Fingerprint for locale detection in TikTok.
 * Target: Method that returns the detected region/locale.
 */
internal val getRegionFingerprint = fingerprint {
    returns("Ljava/lang/String;")
    strings("region", "country", "locale", "geo")
    custom { method, classDef ->
        (method.name.contains("getRegion", ignoreCase = true) ||
         method.name.contains("getCountry", ignoreCase = true) ||
         method.name.contains("getLocale", ignoreCase = true)) &&
        (classDef.type.contains("Config") || classDef.type.contains("Setting") || classDef.type.contains("Geo"))
    }
}

/**
 * Fingerprint for carrier country detection.
 * Target: Methods that get carrier/operator country.
 */
internal val getCarrierCountryFingerprint = fingerprint {
    returns("Ljava/lang/String;")
    custom { method, classDef ->
        (method.name == "getCarrierCountry" ||
         method.name == "getOperatorCountry" ||
         method.name.contains("MccMnc", ignoreCase = true)) &&
        classDef.type.contains("Carrier", ignoreCase = true)
    }
}

/**
 * Fingerprint for IP-based geolocation.
 * Target: Methods that determine region from IP address.
 */
internal val geoIpFingerprint = fingerprint {
    returns("Ljava/lang/String;")
    strings("geoip", "ip_country", "country_code")
    custom { method, classDef ->
        classDef.type.contains("Geo", ignoreCase = true) ||
        method.name.contains("IpCountry", ignoreCase = true)
    }
}

/**
 * Fingerprint for region config initialization.
 * Target: AppConfig or similar that stores region.
 */
internal val regionConfigFingerprint = fingerprint {
    returns("V")
    strings("region", "country_code", "carrier_region")
    custom { method, classDef ->
        (method.name.contains("init", ignoreCase = true) ||
         method.name.contains("set", ignoreCase = true)) &&
        (classDef.type.contains("AppConfig") ||
         classDef.type.contains("RegionConfig") ||
         classDef.type.contains("GeoConfig"))
    }
}

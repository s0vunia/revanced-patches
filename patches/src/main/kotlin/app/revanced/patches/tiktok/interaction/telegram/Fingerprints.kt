package app.revanced.patches.tiktok.interaction.telegram

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for ACLCommonShare.getCode() method.
 * This method is called EARLY to check download permissions - before download UI shows.
 */
internal val aclCommonShareTelegramFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("I")
    custom { method, classDef ->
        classDef.endsWith("/ACLCommonShare;") &&
                method.name == "getCode"
    }
}

/**
 * Fingerprint to hook share panel initialization where Aweme is available.
 * Hooks into SharePanelHelper or similar to capture current video info.
 */
internal val sharePanelAwemeFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC)
    returns("V")
    parameters(
        "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
    )
    custom { method, classDef ->
        // Look for methods that take Aweme as first param and are related to share/download
        classDef.type.contains("Share") || classDef.type.contains("Download") ||
                classDef.type.contains("Panel")
    }
}

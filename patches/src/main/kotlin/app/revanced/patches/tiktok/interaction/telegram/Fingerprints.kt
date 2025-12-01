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

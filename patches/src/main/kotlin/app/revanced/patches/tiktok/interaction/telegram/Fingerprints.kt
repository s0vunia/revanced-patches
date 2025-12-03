package app.revanced.patches.tiktok.interaction.telegram

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for the download URI creation method.
 * This method creates the file URI where the video will be saved.
 * Hooking here allows us to intercept the actual download operation.
 */
internal val downloadUriTelegramFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returns("Landroid/net/Uri;")
    parameters(
        "Landroid/content/Context;",
        "Ljava/lang/String;"
    )
    strings(
        "/",
        "/Camera",
        "/Camera/",
        "video/mp4"
    )
}

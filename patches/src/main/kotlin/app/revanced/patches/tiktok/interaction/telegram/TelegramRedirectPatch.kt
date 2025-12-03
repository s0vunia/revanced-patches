package app.revanced.patches.tiktok.interaction.telegram

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsComposePatch
import app.revanced.patches.tiktok.misc.settings.settingsStatusLoadFingerprint

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/tiktok/download/TelegramRedirect;"

@Suppress("unused")
val telegramRedirectPatch = bytecodePatch(
    name = "Telegram redirect",
    description = "Redirects download button to open a Telegram bot instead of downloading.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsComposePatch,
    )

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Hook into the download URI creation method
        // This is called when actually creating the file to download to
        // p0 = Context, p1 = filename (String)
        // Returns Uri - we return null to block download and redirect to Telegram
        downloadUriTelegramFingerprint.method.addInstructionsWithLabels(
            0,
            """
                # Check if redirect is enabled, if so open Telegram and return null
                invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->onDownloadUri(Landroid/content/Context;)Landroid/net/Uri;
                move-result-object v0
                # If result is non-null sentinel, we handled it - return null to block download
                # If result is null, proceed with normal download
                if-eqz v0, :proceed_download
                const/4 v0, 0x0
                return-object v0
                :proceed_download
                nop
            """,
        )

        // Enable the telegram redirect settings in the settings status
        settingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/revanced/extension/tiktok/settings/SettingsStatus;->enableTelegramRedirect()V",
        )
    }
}

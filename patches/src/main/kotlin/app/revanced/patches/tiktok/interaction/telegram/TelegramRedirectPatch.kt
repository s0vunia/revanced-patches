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
        // Hook into ACLCommonShare.getCode() - called BEFORE download UI shows
        // This intercepts the download permission check early
        // p0 = this (ACLCommonShare object which contains aweme reference)
        aclCommonShareTelegramFingerprint.method.addInstructionsWithLabels(
            0,
            """
                # Try to capture aweme info from ACLCommonShare (p0 = this)
                invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->captureAwemeFromShare(Ljava/lang/Object;)V

                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->onDownloadCheck()I
                move-result v0
                if-eqz v0, :proceed_download
                return v0
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

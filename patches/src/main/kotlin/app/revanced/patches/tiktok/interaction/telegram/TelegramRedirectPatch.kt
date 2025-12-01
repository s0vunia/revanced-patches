package app.revanced.patches.tiktok.interaction.telegram

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsPatch
import app.revanced.patches.tiktok.misc.settings.settingsStatusLoadFingerprint

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/tiktok/download/TelegramRedirect;"

@Suppress("unused")
val telegramRedirectPatch = bytecodePatch(
    name = "Telegram redirect",
    description = "Redirects download button to open a Telegram bot instead of downloading.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4"),
        "com.zhiliaoapp.musically"("36.5.4"),
    )

    execute {
        // Hook into the download URI creation method
        // This method is called when a download is initiated
        // p0 = Context, p1 = path string
        downloadUriTelegramFingerprint.method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->onDownloadClick(Landroid/content/Context;)Z
                move-result v0
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

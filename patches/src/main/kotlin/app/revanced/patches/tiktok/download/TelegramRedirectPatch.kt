package app.revanced.patches.tiktok.download

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsComposePatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/download/TelegramRedirect;"

/**
 * Patch to redirect TikTok download menu to Telegram bot.
 *
 * When user clicks download/save video, instead of downloading locally,
 * it opens the configured Telegram bot with the video URL/ID.
 *
 * This allows you to:
 * - Create your own video downloader bot
 * - Collect analytics on downloads
 * - Serve your own content/ads through the bot
 * - Control the download experience
 */
@Suppress("unused")
val telegramRedirectPatch = bytecodePatch(
    name = "Telegram redirect",
    description = "Redirects video download to your Telegram bot instead of local download.",
) {
    dependsOn(sharedExtensionPatch, settingsComposePatch)

    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
        "com.zhiliaoapp.musically"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
    )

    execute {
        // Hook the download button click handler
        downloadButtonFingerprint.methodOrNull?.apply {
            addInstructionsWithLabels(
                0,
                """
                    # Check if Telegram redirect is enabled
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isRedirectEnabled()Z
                    move-result v0
                    if-eqz v0, :original_download

                    # Get context from the view (p0 is usually View in onClick)
                    invoke-virtual { p0 }, Landroid/view/View;->getContext()Landroid/content/Context;
                    move-result-object v1

                    # Open Telegram bot
                    invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->openTelegramBot(Landroid/content/Context;)V

                    # Return without executing original download
                    return-void

                    :original_download
                    nop
                """
            )
        }

        // Hook the share panel item click - specifically for download option
        sharePanelItemClickFingerprint.methodOrNull?.apply {
            addInstructionsWithLabels(
                0,
                """
                    # Check if this is the download item being clicked
                    invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->isDownloadItem(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :not_download

                    # Check if redirect is enabled
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isRedirectEnabled()Z
                    move-result v0
                    if-eqz v0, :not_download

                    # Get context
                    invoke-virtual { p0 }, Landroid/view/View;->getContext()Landroid/content/Context;
                    move-result-object v1

                    # Get current video info and open Telegram
                    invoke-static { v1 }, $EXTENSION_CLASS_DESCRIPTOR->openTelegramBotWithVideo(Landroid/content/Context;)V
                    return-void

                    :not_download
                    nop
                """
            )
        }

        // Hook the actual download execution to intercept
        downloadExecuteFingerprint.methodOrNull?.apply {
            // p1 is typically the video URL
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->isRedirectEnabled()Z
                    move-result v0
                    if-eqz v0, :do_download

                    # Redirect to Telegram with video URL
                    invoke-static { p0, p1 }, $EXTENSION_CLASS_DESCRIPTOR->redirectToTelegram(Landroid/content/Context;Ljava/lang/String;)V
                    return-void

                    :do_download
                    nop
                """
            )
        }

        // Also hook the share menu click to intercept download early
        shareMenuClickFingerprint.methodOrNull?.apply {
            addInstructionsWithLabels(
                0,
                """
                    # Store current video info when share menu is opened
                    invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->storeCurrentVideoInfo(Ljava/lang/Object;)V
                """
            )
        }

        // Make download always available (remove restrictions)
        aclCommonShareFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """
            )
        }
    }
}

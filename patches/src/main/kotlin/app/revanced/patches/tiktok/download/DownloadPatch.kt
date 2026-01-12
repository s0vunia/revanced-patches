package app.revanced.patches.tiktok.download

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsComposePatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/download/DownloadEnhancer;"

/**
 * Patch to enhance TikTok video download functionality.
 *
 * This patch enables:
 * - Watermark-free video downloads
 * - Download button always visible (bypass ACL restrictions)
 * - Force no-watermark download type
 */
@Suppress("unused")
val downloadPatch = bytecodePatch(
    name = "Download enhancer",
    description = "Enables watermark-free video downloads and bypasses download restrictions.",
) {
    dependsOn(sharedExtensionPatch, settingsComposePatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Method 1: Force ACL to always allow download
        aclCommonShareFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldBypassAcl()Z
                    move-result v0
                    if-eqz v0, :use_original
                    const/4 v0, 0x0
                    return v0
                    :use_original
                    nop
                """
            )
        }

        // Method 2: Hook download URL getter to return no-watermark URL
        getVideoUrlFingerprint.methodOrNull?.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->getNoWatermarkUrl(Ljava/lang/Object;)Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :use_original
                    return-object v0
                    :use_original
                    nop
                """
            )
        }

        // Method 3: Hook download execution to use no-watermark URL
        downloadExecuteFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->processDownloadUrl(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                """
            )
        }

        // Method 4: Store video info when share menu opens for later use
        shareMenuClickFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->onShareMenuOpened(Ljava/lang/Object;)V
                """
            )
        }
    }
}

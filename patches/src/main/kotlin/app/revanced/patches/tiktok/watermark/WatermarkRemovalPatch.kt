package app.revanced.patches.tiktok.watermark

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsComposePatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/watermark/WatermarkRemoval;"

/**
 * Patch to remove watermarks from TikTok videos and images.
 *
 * This patch works by:
 * 1. Hooking ByteWatermark constructor to set dimensions to 0
 * 2. Redirecting video download URLs to no-watermark variants
 * 3. Hiding watermark UI views
 * 4. Skipping image watermark application
 */
@Suppress("unused")
val watermarkRemovalPatch = bytecodePatch(
    name = "Remove watermark",
    description = "Removes TikTok watermarks from videos and images for cleaner downloads.",
) {
    dependsOn(sharedExtensionPatch, settingsComposePatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Method 1: Hook ByteWatermark constructor - set dimensions to 0 when removal enabled
        byteWatermarkFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldRemoveWatermark()Z
                    move-result v0
                    if-eqz v0, :keep_watermark
                    const/4 p1, 0x0
                    const/4 p2, 0x0
                    const/4 p3, 0x0
                    const/4 p4, 0x0
                    :keep_watermark
                    nop
                """
            )
        }

        // Method 2: Hook video download address getter - return no-watermark URL
        // Use /range because parameter registers may exceed v15
        videoDownloadAddrFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->getNoWatermarkDownloadAddr(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v0
                    if-eqz v0, :use_original
                    return-object v0
                    :use_original
                    nop
                """
            )
        }

        // Method 3: Hook H265 play address for no-watermark playback
        videoPlayAddrH265Fingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->getNoWatermarkPlayAddr(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v0
                    if-eqz v0, :use_original
                    return-object v0
                    :use_original
                    nop
                """
            )
        }

        // Method 4: Hide watermark view by intercepting setVisibility
        watermarkViewFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldRemoveWatermark()Z
                    move-result v0
                    if-eqz v0, :show_watermark
                    const/16 p1, 0x8
                    :show_watermark
                    nop
                """
            )
        }

        // Method 5: Skip image watermark application
        imageWatermarkFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldRemoveImageWatermark()Z
                    move-result v0
                    if-eqz v0, :apply_watermark
                    return-object p1
                    :apply_watermark
                    nop
                """
            )
        }

        // Method 6: Hook video URI getter for clean URLs
        videoUriFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->getCleanVideoUri(Ljava/lang/Object;)Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :use_original
                    return-object v0
                    :use_original
                    nop
                """
            )
        }
    }
}

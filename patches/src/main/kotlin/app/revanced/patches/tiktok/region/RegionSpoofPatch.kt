package app.revanced.patches.tiktok.region

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.patches.tiktok.misc.settings.settingsComposePatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/region/RegionSpoof;"

/**
 * Patch to spoof region detection in TikTok.
 *
 * This patch allows users to:
 * - Override SIM country code detection
 * - Override network country detection
 * - Spoof region/locale settings
 * - Access region-locked content
 */
@Suppress("unused")
val regionSpoofPatch = bytecodePatch(
    name = "Region spoof",
    description = "Allows spoofing the detected region to access region-locked content.",
) {
    dependsOn(sharedExtensionPatch, settingsComposePatch)

    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
        "com.zhiliaoapp.musically"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
    )

    execute {
        // Hook region getter methods
        getRegionFingerprint.methodOrNull?.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getSpoofedRegion()Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :use_original
                    return-object v0
                    :use_original
                    nop
                """
            )
        }

        // Hook carrier country detection
        getCarrierCountryFingerprint.methodOrNull?.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getSpoofedRegion()Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :use_original
                    return-object v0
                    :use_original
                    nop
                """
            )
        }

        // Hook geo IP detection
        geoIpFingerprint.methodOrNull?.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getSpoofedRegion()Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :use_original
                    return-object v0
                    :use_original
                    nop
                """
            )
        }

        // Hook region config initialization
        regionConfigFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->onRegionConfigInit()V
                """
            )
        }
    }
}

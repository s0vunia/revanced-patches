package app.revanced.patches.tiktok.misc.extension

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.misc.extension.sharedExtensionPatch as baseSharedExtensionPatch

/**
 * Shared extension patch that includes the extension code into the APK.
 * This must be a dependency of all patches that use extension code.
 */
val sharedExtensionPatch = bytecodePatch(
    name = "TikTok extension",
    description = "Includes shared extension code for TikTok patches.",
) {
    dependsOn(baseSharedExtensionPatch("tiktok"))
}

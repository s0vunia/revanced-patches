package app.revanced.patches.tiktok.nativelib

import app.revanced.patcher.patch.rawResourcePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.inputStreamFromBundledResource
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

/**
 * Native Library Replacement Patch
 *
 * Replaces the original libtigrik.so with our custom TikMod implementation.
 * This removes all cloud control features:
 * - Remote kill switch (KillAppReceiver)
 * - Cloud banner ads
 * - Remote configuration fetching
 *
 * While keeping useful features:
 * - Watermark removal
 * - Ad blocking
 * - Feed filtering
 * - Download features
 * - Local-only settings
 */
@Suppress("unused")
val nativeLibReplacementPatch = rawResourcePatch(
    name = "Native library replacement",
    description = "Replaces libtigrik.so with TikMod (no cloud control, no kill switch, no remote banners).",
) {
    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
        "com.zhiliaoapp.musically"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
    )

    execute {
        val architectures = listOf("arm64-v8a", "armeabi-v7a")

        for (arch in architectures) {
            // Get the native library from bundled resources
            val inputStream = inputStreamFromBundledResource("tiktok/nativelib/$arch", "libtigrik.so")

            if (inputStream != null) {
                // Target path in the APK
                val targetDir = get("lib/$arch", true)
                targetDir.mkdirs()

                val targetFile = File(targetDir, "libtigrik.so")

                // Copy the replacement library
                inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                println("Replaced libtigrik.so for $arch")
            } else {
                println("Warning: Native library not found for $arch - skipping")
            }
        }
    }
}

/**
 * Alternative patch that disables the original native library loading
 * and uses pure Java/Kotlin implementation instead.
 *
 * This is useful if you don't want to deal with native compilation.
 */
@Suppress("unused")
val disableNativeLibPatch = resourcePatch(
    name = "Disable native lib",
    description = "Disables libtigrik.so loading (mod features won't work, but removes cloud control).",
) {
    compatibleWith(
        "com.ss.android.ugc.trill"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
        "com.zhiliaoapp.musically"("36.5.4", "37.0.0", "37.1.0", "37.2.0", "38.0.0"),
    )

    execute {
        // This patch works by modifying the AndroidManifest to remove
        // the tigrik receiver, effectively disabling cloud control
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement

            // Find and remove KillAppReceiver
            val receivers = manifest.getElementsByTagName("receiver")
            for (i in 0 until receivers.length) {
                val receiver = receivers.item(i)
                val name = receiver.attributes.getNamedItem("android:name")?.nodeValue
                if (name?.contains("KillAppReceiver") == true ||
                    name?.contains("tigrik") == true) {
                    receiver.parentNode.removeChild(receiver)
                    println("Removed receiver: $name")
                }
            }
        }
    }
}

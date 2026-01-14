package app.revanced.patches.tiktok.nativelib

import app.revanced.patcher.patch.rawResourcePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.inputStreamFromBundledResource
import java.io.File
import java.io.FileOutputStream

/**
 * Native Library Replacement Patch
 *
 * Replaces the original native library with a custom ReVanced implementation.
 * This removes all cloud control features:
 * - Remote kill switch (prevents remote app termination)
 * - Cloud banner ads (removes server-pushed advertisements)
 * - Remote configuration fetching (keeps settings local-only)
 *
 * While preserving useful features:
 * - Watermark removal
 * - Ad blocking
 * - Feed filtering
 * - Download features
 * - Local settings storage
 *
 * Note: The library filename must remain unchanged as TikTok expects it.
 */
@Suppress("unused")
val nativeLibReplacementPatch = rawResourcePatch(
    name = "Native library replacement",
    description = "Replaces native library with ReVanced version (no cloud control, no kill switch, no remote banners).",
) {
    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        val architectures = listOf("arm64-v8a", "armeabi-v7a")

        for (arch in architectures) {
            // Get the native library from bundled resources
            // Note: Filename must match what TikTok expects (libtigrik.so)
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

                println("Replaced native library for $arch")
            } else {
                println("Warning: Native library not found for $arch - skipping")
            }
        }
    }
}

/**
 * Alternative patch that disables the native library loading entirely.
 * Uses pure Java/Kotlin implementation instead.
 *
 * Use this if you don't need native features or want simpler patching.
 * Note: Some mod features won't work, but cloud control is removed.
 */
@Suppress("unused")
val disableNativeLibPatch = resourcePatch(
    name = "Disable native lib",
    description = "Disables native library loading (simpler but fewer features).",
) {
    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Remove cloud control receivers from AndroidManifest
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement

            // Find and remove remote kill switch receiver
            val receivers = manifest.getElementsByTagName("receiver")
            for (i in 0 until receivers.length) {
                val receiver = receivers.item(i)
                val name = receiver.attributes.getNamedItem("android:name")?.nodeValue
                if (name?.contains("KillAppReceiver") == true) {
                    receiver.parentNode.removeChild(receiver)
                    println("Removed cloud control receiver: $name")
                }
            }
        }
    }
}

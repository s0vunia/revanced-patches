package app.revanced.patches.tiktok.banner

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/banner/BannerManager;"

/**
 * Banner Patch - Shows custom announcements on app start.
 *
 * NOTE: MainActivity.onCreate() is NATIVE in TikTok 43.x, so we hook
 * JatoInitTask.run() instead, which runs during app startup and has Context.
 *
 * WHEN BANNER SHOWS:
 * 1. App starts (JatoInitTask.run is called with Context)
 * 2. Remote config is ENABLED in settings
 * 3. Config URL is set by user
 * 4. announcement.enabled = true in config
 * 5. User hasn't dismissed this announcement
 *
 * WHEN BANNER DOES NOT SHOW:
 * - Remote config disabled (default)
 * - No config URL configured
 * - announcement.enabled = false
 * - Already dismissed by user
 * - Network error and no cache
 */
@Suppress("unused")
val bannerPatch = bytecodePatch(
    name = "Custom banner",
    description = "Shows custom announcements from remote config on app start.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Hook AwemeHostApplication.onCreate() - Application class, always exists
        awemeHostApplicationOnCreateFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                "invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->onAppStarted(Landroid/content/Context;)V"
            )
            println("Banner patch: Hooked AwemeHostApplication.onCreate()")
        } ?: println("Banner patch: AwemeHostApplication not found - banner will not show")
    }
}

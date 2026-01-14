package app.revanced.patches.tiktok.banner

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/banner/BannerManager;"

/**
 * Banner Patch - Shows custom announcements on app start.
 *
 * This patch hooks into MainActivity.onCreate() and calls BannerManager
 * to display announcements from user-configured remote config.
 *
 * WHEN BANNER SHOWS:
 * 1. App starts (MainActivity.onCreate is called)
 * 2. Remote config is ENABLED in settings
 * 3. Config URL is set by user (e.g., https://yoursite.com/config.json)
 * 4. Config has announcement.enabled = true
 * 5. User has NOT dismissed this specific announcement (tracked by dismissKey)
 *
 * WHEN BANNER DOES NOT SHOW:
 * - Remote config disabled (default state)
 * - No config URL configured
 * - announcement.enabled = false in config
 * - User already clicked "OK" or "Don't show again"
 * - Network error and no cached config
 * - Banner already shown this session
 *
 * Example config.json:
 * {
 *   "announcement": {
 *     "enabled": true,
 *     "title": "Welcome!",
 *     "message": "Thanks for using TikTok ReVanced!",
 *     "url": "https://t.me/your_channel",
 *     "dismissKey": "welcome_v1"
 *   }
 * }
 *
 * To show a NEW banner after user dismissed, change the dismissKey
 * (e.g., "welcome_v2", "update_1.1", etc.)
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
        // Hook MainActivity.onCreate() to show banner
        val mainActivityMethod = mainActivityOnCreateFingerprint.methodOrNull
            ?: mainActivityLiteOnCreateFingerprint.methodOrNull

        mainActivityMethod?.apply {
            // Inject at the end of onCreate (after super.onCreate and UI setup)
            // We add at index 0 but with a delay in Java code, so it doesn't block UI
            addInstructions(
                implementation!!.instructions.size - 1,
                """
                    # Call BannerManager.onMainActivityCreated(this)
                    invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->onMainActivityCreated(Landroid/app/Activity;)V
                """
            )
            println("Banner patch: Hooked MainActivity.onCreate()")
        } ?: println("Banner patch: MainActivity.onCreate() not found - banner will not show")
    }
}

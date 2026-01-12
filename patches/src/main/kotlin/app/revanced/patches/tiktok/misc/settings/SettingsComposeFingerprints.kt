package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for Settings Compose Fragment onCreateView()
 * - TikTok 42.x: SettingsComposeVersionFragment
 * - TikTok 43.x: SettingsComposeRvmpFragment
 */
internal val settingsComposeOnCreateViewFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Landroid/view/View;")
    parameters("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;")
    custom { method, classDef ->
        (classDef.type.endsWith("/SettingsComposeVersionFragment;") ||
            classDef.type.endsWith("/SettingsComposeRvmpFragment;")) &&
            method.name == "onCreateView"
    }
}

/**
 * Fingerprint for Settings Compose Fragment onViewCreated()
 * - TikTok 42.x: SettingsComposeVersionFragment
 * - TikTok 43.x: SettingsComposeRvmpFragment
 */
internal val settingsComposeOnViewCreatedFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("V")
    parameters("Landroid/view/View;", "Landroid/os/Bundle;")
    custom { method, classDef ->
        (classDef.type.endsWith("/SettingsComposeVersionFragment;") ||
            classDef.type.endsWith("/SettingsComposeRvmpFragment;")) &&
            method.name == "onViewCreated"
    }
}

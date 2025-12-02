package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for SettingsComposeVersionFragment.onCreateView()
 * This is the new Compose-based settings UI introduced in TikTok 42+
 */
internal val settingsComposeOnCreateViewFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Landroid/view/View;")
    parameters("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;", "Landroid/os/Bundle;")
    custom { method, classDef ->
        classDef.type.endsWith("/SettingsComposeVersionFragment;") &&
            method.name == "onCreateView"
    }
}

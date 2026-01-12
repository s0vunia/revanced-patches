package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/settings/SettingsComposeHook;"

/**
 * Patch for TikTok 42+ which uses Jetpack Compose for settings UI.
 * Injects a "Tralalelo Settings" button at the top of the settings screen.
 */
val settingsComposePatch = bytecodePatch(
    name = "Settings (Compose)",
    description = "Adds Tralalelo settings to TikTok's Compose-based settings UI.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        // Hook SettingsComposeVersionFragment.onViewCreated()
        // This is called after the view is attached, so we can modify the view hierarchy
        // Use invoke-static/range because p1 may map to v17+ which exceeds invoke-static's v0-v15 limit
        settingsComposeOnViewCreatedFingerprint.method.addInstructions(
            0,
            "invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS_DESCRIPTOR->onViewCreated(Landroid/view/View;)V",
        )

        // Hook AdPersonalizationActivity.onCreate() to show our settings when opened with revanced=true
        adPersonalizationActivityOnCreateFingerprint.method.apply {
            val initializeSettingsIndex = implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_SUPER
            } + 1

            // Use p0 (this) for the activity reference and v0 for the result
            // v0 is always safe to use as a temporary register
            // Use invoke-static/range because p0 may map to v23+ which exceeds invoke-static's v0-v15 limit
            addInstructionsWithLabels(
                initializeSettingsIndex,
                """
                    invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->initializeSettings(Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;)Z
                    move-result v0
                    if-eqz v0, :do_not_open
                    return-void
                """.trimIndent(),
                ExternalLabel("do_not_open", getInstruction(initializeSettingsIndex)),
            )
        }
    }
}

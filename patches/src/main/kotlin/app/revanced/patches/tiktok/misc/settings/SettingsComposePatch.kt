package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

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
        // Hook SettingsComposeVersionFragment.onCreateView()
        // This method returns a ComposeView that we wrap with our settings button
        settingsComposeOnCreateViewFingerprint.method.apply {
            val instructions = implementation!!.instructions

            // Find the return instruction (last instruction that returns a view)
            val returnIndex = instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
            val viewRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            // Before return, wrap the view with our settings button
            addInstructions(
                returnIndex,
                """
                    invoke-static {v$viewRegister}, $EXTENSION_CLASS_DESCRIPTOR->wrapSettingsView(Landroid/view/View;)Landroid/view/View;
                    move-result-object v$viewRegister
                """,
            )
        }

        // Hook AdPersonalizationActivity.onCreate() to show our settings when opened with revanced=true
        adPersonalizationActivityOnCreateFingerprint.method.apply {
            val initializeSettingsIndex = implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_SUPER
            } + 1

            val thisRegister = getInstruction<Instruction35c>(initializeSettingsIndex - 1).registerC
            val usableRegister = implementation!!.registerCount - parameters.size - 2

            addInstructionsWithLabels(
                initializeSettingsIndex,
                """
                    invoke-static {v$thisRegister}, $EXTENSION_CLASS_DESCRIPTOR->initializeSettings(Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;)Z
                    move-result v$usableRegister
                    if-eqz v$usableRegister, :do_not_open
                    return-void
                """,
                ExternalLabel("do_not_open", getInstruction(initializeSettingsIndex)),
            )
        }
    }
}

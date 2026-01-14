package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.layout.branding.addBrandLicensePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/settings/TikTokActivityHook;"

/**
 * Legacy settings patch for TikTok versions < 42.x that use SettingNewVersionFragment.
 * For TikTok 42.x+ use settingsComposePatch instead.
 *
 * This patch gracefully skips if the legacy settings classes are not found (TikTok 43.x removed them).
 */
val settingsPatch = bytecodePatch(
    name = "Settings (Legacy)",
    description = "Adds ReVanced settings to TikTok (legacy UI for versions < 42.x).",
) {
    dependsOn(sharedExtensionPatch, addBrandLicensePatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    execute {
        val initializeSettingsMethodDescriptor =
            "$EXTENSION_CLASS_DESCRIPTOR->initialize(" +
                "Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;" +
                ")Z"

        val createSettingsEntryMethodDescriptor =
            "$EXTENSION_CLASS_DESCRIPTOR->createSettingsEntry(" +
                "Ljava/lang/String;" +
                "Ljava/lang/String;" +
                ")Ljava/lang/Object;"

        fun String.toClassName(): String = substring(1, this.length - 1).replace("/", ".")

        // Legacy settings entry injection - only works on TikTok < 43.x
        // TikTok 43.x removed SettingNewVersionFragment, so these fingerprints won't match
        val settingsEntryClass = settingsEntryFingerprint.originalClassDefOrNull
        val settingsEntryInfoClass = settingsEntryInfoFingerprint.originalClassDefOrNull
        val addSettingsEntryMethod = addSettingsEntryFingerprint.methodOrNull

        if (settingsEntryClass != null && settingsEntryInfoClass != null && addSettingsEntryMethod != null) {
            val settingsButtonClass = settingsEntryClass.type.toClassName()
            val settingsButtonInfoClass = settingsEntryInfoClass.type.toClassName()

            // Create a settings entry for 'revanced settings' and add it to settings fragment
            addSettingsEntryMethod.apply {
                val markIndex = implementation!!.instructions.indexOfFirst {
                    it.opcode == Opcode.IGET_OBJECT && ((it as Instruction22c).reference as FieldReference).name == "headerUnit"
                }

                if (markIndex >= 0) {
                    val getUnitManager = getInstruction(markIndex + 2)
                    val addEntry = getInstruction(markIndex + 1)

                    addInstructions(
                        markIndex + 2,
                        listOf(
                            getUnitManager,
                            addEntry,
                        ),
                    )

                    addInstructions(
                        markIndex + 2,
                        """
                            const-string v0, "$settingsButtonClass"
                            const-string v1, "$settingsButtonInfoClass"
                            invoke-static {v0, v1}, $createSettingsEntryMethodDescriptor
                            move-result-object v0
                            check-cast v0, ${settingsEntryClass.type}
                        """,
                    )
                }
            }
        }
        // If legacy fingerprints don't match, the patch simply skips this part.
        // TikTok 42.x+ should use settingsComposePatch instead.

        // Initialize the settings menu once the replaced setting entry is clicked.
        // AdPersonalizationActivity exists in all TikTok versions, so this always works.
        adPersonalizationActivityOnCreateFingerprint.methodOrNull?.apply {
            val initializeSettingsIndex = implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_SUPER
            } + 1

            // Use p0 (this) for the activity reference and v0 for the result
            // v0 is always safe to use as a temporary register
            // Use invoke-static/range because p0 may map to v23+ which exceeds invoke-static's v0-v15 limit
            addInstructionsWithLabels(
                initializeSettingsIndex,
                """
                    invoke-static/range { p0 .. p0 }, $initializeSettingsMethodDescriptor
                    move-result v0
                    if-eqz v0, :do_not_open
                    return-void
                """.trimIndent(),
                ExternalLabel("do_not_open", getInstruction(initializeSettingsIndex)),
            )
        }
    }
}

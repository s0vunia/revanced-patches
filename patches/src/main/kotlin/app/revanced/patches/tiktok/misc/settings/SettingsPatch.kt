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

val settingsPatch = bytecodePatch(
    name = "Settings",
    description = "Adds ReVanced settings to TikTok.",
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

        // Find the class name of classes which construct a settings entry
        val settingsButtonClass = settingsEntryFingerprint.originalClassDef.type.toClassName()
        val settingsButtonInfoClass = settingsEntryInfoFingerprint.originalClassDef.type.toClassName()

        // Create a settings entry for 'revanced settings' and add it to settings fragment
        addSettingsEntryFingerprint.method.apply {
            val markIndex = implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.IGET_OBJECT && ((it as Instruction22c).reference as FieldReference).name == "headerUnit"
            }

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
                    check-cast v0, ${settingsEntryFingerprint.originalClassDef.type}
                """,
            )
        }

        // Initialize the settings menu once the replaced setting entry is clicked.
        adPersonalizationActivityOnCreateFingerprint.method.apply {
            val initializeSettingsIndex = implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.INVOKE_SUPER
            } + 1

            // Use p0 (this) for the activity reference and v0 for the result
            // v0 is always safe to use as a temporary register
            addInstructionsWithLabels(
                initializeSettingsIndex,
                """
                    invoke-static {p0}, $initializeSettingsMethodDescriptor
                    move-result v0
                    if-eqz v0, :do_not_open
                    return-void
                """,
                ExternalLabel("do_not_open", getInstruction(initializeSettingsIndex)),
            )
        }
    }
}
